package com.jpmc.midascore;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceController {

    @Autowired
    private DatabaseConduit databaseConduit;

    @GetMapping("/balance")
    public Balance getUserBalance(@RequestParam long userId) {
        // Find the user by ID
        UserRecord userRecord = databaseConduit.findUserById(userId);

        // Return the user's balance, or 0 if user is not found
        if (userRecord != null) {
            return new Balance(userRecord.getBalance());
        } else {
            return new Balance(0);
        }
    }
}



