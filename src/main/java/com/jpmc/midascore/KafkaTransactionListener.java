package com.jpmc.midascore;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KafkaTransactionListener {

    @Autowired
    private DatabaseConduit databaseConduit;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${incentive.api.url:http://localhost:8080/incentive}")  // Corrected value annotation
    private String incentiveApiUrl;

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "transaction-group")
    public void listen(Transaction transaction) {
        // Retrieve sender and recipient records
        UserRecord sender = databaseConduit.findUserById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.findUserById(transaction.getRecipientId());

        if (sender != null && recipient != null && sender.getBalance() >= transaction.getAmount()) {
            try {
                // Post transaction to Incentives API
                ResponseEntity<Incentive> response = restTemplate.postForEntity(incentiveApiUrl, transaction, Incentive.class);

                if (response.getBody() != null) {
                    float incentiveAmount = response.getBody().getAmount();

                    // Adjust balances
                    sender.setBalance(sender.getBalance() - transaction.getAmount());
                    recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

                    // Save updated user records
                    databaseConduit.save(sender);
                    databaseConduit.save(recipient);

                    // Save the transaction record with incentive
                    TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
                    databaseConduit.saveTransaction(transactionRecord);

                    // Print the sender and recipient new balances
                    System.out.println(sender.getName() + " new balance: " + sender.getBalance());
                    System.out.println(recipient.getName() + " new balance: " + recipient.getBalance());
                } else {
                    System.out.println("Incentive API returned null or invalid response.");
                }
            } catch (Exception e) {
                System.out.println("Error calling Incentives API: " + e.getMessage());
            }
        } else {
            System.out.println("Transaction invalid: " + transaction.toString());
        }
    }
}


