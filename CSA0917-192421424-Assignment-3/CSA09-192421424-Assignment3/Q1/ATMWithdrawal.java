import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Checked exception: the customer asked for more than the balance (recoverable, caller must decide). */
class InsufficientFundsException extends Exception {
    InsufficientFundsException(String message) { super(message); }
}

/** Checked exception: account number is well formed but does not exist. */
class AccountNotFoundException extends Exception {
    AccountNotFoundException(String message) { super(message); }
}

/**
 * Q1 - Bank ATM withdrawal with try / catch (multi-catch) / finally.
 *
 * Run:  java ATMWithdrawal            -> runs the 8-row test table
 *       java ATMWithdrawal uncaught   -> shows the stack trace of an UNCAUGHT exception
 */
public class ATMWithdrawal {

    static final Map<Integer, Integer> balances = new HashMap<>();
    static int finallyCount = 0;

    static {
        balances.put(1001, 5000);
        balances.put(1002, 200);
    }

    /** Reads the card-reader file. FileNotFoundException (checked) if the file is missing. */
    static String readCard(String cardFile) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(cardFile))) {
            return r.readLine();
        }
    }

    /** Performs one withdrawal and returns the name of the catch block that handled it ("none" if success). */
    static String withdraw(String cardFile, String accText, String amtText) {
        String handledBy = "none (success)";
        try {
            readCard(cardFile);                              // checked: FileNotFoundException / IOException
            int account = Integer.parseInt(accText.trim());  // unchecked: NumberFormatException, NullPointerException
            int amount = Integer.parseInt(amtText.trim());   // unchecked: NumberFormatException, NullPointerException
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");   // unchecked
            }
            Integer balance = balances.get(account);
            if (balance == null) {
                throw new AccountNotFoundException("No such account: " + account);   // checked
            }
            if (amount > balance) {
                throw new InsufficientFundsException("Balance " + balance + " is less than " + amount); // checked
            }
            balances.put(account, balance - amount);
            System.out.println("   Dispensed " + amount + ". New balance = " + balances.get(account));
        }
        // ORDER MATTERS: specific (child) types first, general (parent) types later.
        catch (NumberFormatException | NullPointerException e) {   // multi-catch: two unrelated types, same action
            handledBy = "catch (NumberFormatException | NullPointerException)";
            System.out.println("   Invalid input: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        } catch (IllegalArgumentException e) {                    // parent of NumberFormatException -> must come AFTER it
            handledBy = "catch (IllegalArgumentException)";
            System.out.println("   Invalid amount: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            handledBy = "catch (InsufficientFundsException)";
            System.out.println("   Declined: " + e.getMessage());
        } catch (AccountNotFoundException e) {
            handledBy = "catch (AccountNotFoundException)";
            System.out.println("   Declined: " + e.getMessage());
        } catch (FileNotFoundException e) {                       // child of IOException -> before IOException
            handledBy = "catch (FileNotFoundException)";
            System.out.println("   Card reader file missing: " + e.getMessage());
        } catch (IOException e) {
            handledBy = "catch (IOException)";
            System.out.println("   Card reader I/O error: " + e.getMessage());
        } finally {
            finallyCount++;
            System.out.println("   [finally] card returned / session closed (finally runs so far: " + finallyCount + ")");
        }
        return handledBy;
    }

    public static void main(String[] args) throws Exception {
        // Make sure the card file exists for the "good" rows.
        java.nio.file.Files.writeString(java.nio.file.Path.of("card.txt"), "CARD-1001\n");

        if (args.length > 0 && args[0].equals("uncaught")) {
            System.out.println("Uncaught demo: nothing catches the NumberFormatException below");
            int amount = Integer.parseInt("12x");     // not inside try/catch -> program terminates with a stack trace
            System.out.println(amount);
            return;
        }

        String[][] tests = {
            // card file, account, amount, description
            {"card.txt",    "1001", "500",   "T1 valid withdrawal"},
            {"card.txt",    "abc",  "100",   "T2 letters instead of number"},
            {"card.txt",    "1001", "-50",   "T3 negative amount"},
            {"card.txt",    "1002", "9999",  "T4 amount above balance"},
            {"missing.txt", "1001", "100",   "T5 card-reader file missing"},
            {"card.txt",    "7777", "100",   "T6 unknown account number"},
            {"card.txt",    null,   "100",   "T7 null account input"},
            {"card.txt",    "1001", "0",     "T8 zero amount"},
        };

        System.out.println("=== Q1 ATM test table ===");
        for (String[] t : tests) {
            System.out.println(t[3] + "  (card=" + t[0] + ", account=" + t[1] + ", amount=" + t[2] + ")");
            String by = withdraw(t[0], t[1], t[2]);
            System.out.println("   => handled by: " + by);
            System.out.println();
        }
        System.out.println("Total withdrawals attempted = " + tests.length + ", finally blocks executed = " + finallyCount);
    }
}
