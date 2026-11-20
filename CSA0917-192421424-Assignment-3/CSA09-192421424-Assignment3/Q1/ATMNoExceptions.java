import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Q1 - Alternative concept: no exceptions at all. Every input is checked in advance with
 * Scanner.hasNextInt() and failures are reported with error codes (-1, -2, ...).
 */
public class ATMNoExceptions {
    static final int OK = 0, BAD_ACCOUNT = -1, BAD_AMOUNT = -2, NEGATIVE = -3, NO_ACCOUNT = -4, NO_FUNDS = -5, NO_CARD = -6;
    static final Map<Integer, Integer> balances = new HashMap<>(Map.of(1001, 5000, 1002, 200));

    static int withdraw(boolean cardPresent, String accText, String amtText) {
        if (!cardPresent) return NO_CARD;                       // check 1
        if (accText == null) return BAD_ACCOUNT;                // check 2
        Scanner a = new Scanner(accText.trim());
        if (!a.hasNextInt()) return BAD_ACCOUNT;                // check 3
        int account = a.nextInt();
        if (amtText == null) return BAD_AMOUNT;                 // check 4
        Scanner m = new Scanner(amtText.trim());
        if (!m.hasNextInt()) return BAD_AMOUNT;                 // check 5
        int amount = m.nextInt();
        if (amount <= 0) return NEGATIVE;                       // check 6
        Integer balance = balances.get(account);
        if (balance == null) return NO_ACCOUNT;                 // check 7
        if (amount > balance) return NO_FUNDS;                  // check 8
        balances.put(account, balance - amount);
        return OK;
    }

    public static void main(String[] args) {
        Object[][] tests = {
            {true, "1001", "500"}, {true, "abc", "100"}, {true, "1001", "-50"}, {true, "1002", "9999"},
            {false, "1001", "100"}, {true, "7777", "100"}, {true, null, "100"}, {true, "1001", "0"}
        };
        for (Object[] t : tests) {
            int code = withdraw((Boolean) t[0], (String) t[1], (String) t[2]);
            System.out.println("card=" + t[0] + " account=" + t[1] + " amount=" + t[2] + " -> code " + code);
        }
    }
}
