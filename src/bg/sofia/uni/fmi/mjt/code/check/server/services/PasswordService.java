package bg.sofia.uni.fmi.mjt.code.check.server.services;

import bg.sofia.uni.fmi.mjt.code.check.server.services.hash.algorithm.HashAlgorithm;

/**
 * Real life example will include salting as well
 * */
public class PasswordService {
    public String hashPassword(String password, HashAlgorithm algorithm) {
        return algorithm.hash(password);
    }

    public boolean checkPassword(String plainPassword, String hashedPassword, HashAlgorithm algorithm) {
        return algorithm.verify(plainPassword, hashedPassword);
    }
}
