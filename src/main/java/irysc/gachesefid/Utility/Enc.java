package irysc.gachesefid.Utility;

import org.bson.types.ObjectId;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Base64;

import static irysc.gachesefid.Utility.StaticValues.KEY;

public class Enc {

    private static Cipher cipher;
    private static Cipher dCipher;
    private static byte[] encodedKey;
    private static SecretKey key;

    public static void init() {
        try {

            encodedKey = KEY.getBytes("UTF-8");
            key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");

            byte[] ivBytes = new byte[] {
                    0x11, 0x00, 0x00, 0x10, 0x12, 0x13, 0x14, 0x13,
                    0x23, 0x43, 0x12, 0x11, 0x08, 0x76, 0x12, 0x32
            };

            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            dCipher = Cipher.getInstance("AES/CTR/NoPadding");
            dCipher.init(Cipher.DECRYPT_MODE, key, iv);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encryptObject(Serializable object) throws IOException, IllegalBlockSizeException {

        SealedObject sealed = new SealedObject(object, cipher);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);

        ObjectOutputStream oos = new ObjectOutputStream(cipherOutputStream);
        oos.writeObject( sealed );
        cipherOutputStream.close();

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static Ticket decryptObject(String e) throws ClassNotFoundException, BadPaddingException, IllegalBlockSizeException,
            IOException {

        ObjectInputStream inputStream = new ObjectInputStream(new CipherInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(e)), dCipher));
        Ticket t = (Ticket) ((SealedObject) inputStream.readObject()).getObject(cipher);
        inputStream.close();

        return t;
    }

    public static class Ticket implements Serializable {

        public long time;
        public String newMail;
        public String username;
        public ObjectId userId;

        public Ticket(String newMail, String username, ObjectId userId) {
            this.newMail = newMail;
            this.username = username;
            this.userId = userId;
            this.time = System.currentTimeMillis();
        }

    }
}
