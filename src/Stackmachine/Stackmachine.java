package Stackmachine;

import java.util.ArrayList;

public class Stackmachine {
    public String getCompOPByteCode(String compOp) {
        return switch (compOp) {
            case "==" -> IF_ICMPNE;
            case "!=" -> IF_ICMPEQ;
            case "<" -> IF_ICMPGE;
            case ">" -> IF_ICMPLE;
            case "<=" -> IF_ICMPGT;
            case ">=" -> IF_ICMPLT;
            default -> "Error";
        };
    }

    public ArrayList<String> calculateJumpBytesForIF(ArrayList<String> byteCode, String currentLabel) {
        ArrayList<String> newByteCode = new ArrayList<>(byteCode);

        if (newByteCode.contains(currentLabel + "ELSESTART")) {
            int indexOfStartElse = newByteCode.indexOf(currentLabel + "ELSESTART");
            int indexOfEndElse = newByteCode.indexOf(currentLabel + "ELSEEND");

            int additionalBytes = 0;
            for (int i = newByteCode.indexOf(currentLabel + "ELSESTART"); i < newByteCode.indexOf(currentLabel + "ELSEEND"); i++) {
                if (newByteCode.get(i).length() == 4 && !checkIfLabel(newByteCode.get(i))) {
                    additionalBytes++;
                }
                if (newByteCode.get(i).contains("(")) {
                    additionalBytes += 2;
                }
                if (newByteCode.get(i).contains("[")) {
                    additionalBytes += 2; // If we reach GETSTATIC or PUTSTATIC, add the two additional Bytes
                }
            }

            ArrayList<String> firstElse = new ArrayList<>(newByteCode.subList(0, indexOfStartElse + 2));
            ArrayList<String> secondElse = new ArrayList<>(newByteCode.subList(indexOfStartElse + 2, newByteCode.size()));

            firstElse.add(String.format("%1$04X", indexOfEndElse - indexOfStartElse + 1 + additionalBytes));

            newByteCode = new ArrayList<>(firstElse);
            newByteCode.addAll(secondElse);
        }

        int indexOfStart = newByteCode.indexOf(currentLabel + "IFSTART");
        int indexOfEnd = newByteCode.indexOf(currentLabel + "IFEND");

        int additionalBytes = 0;
        for (int i = newByteCode.indexOf(currentLabel + "IFSTART"); i < newByteCode.indexOf(currentLabel + "IFEND"); i++) {
            if (newByteCode.get(i).length() == 4 && !checkIfLabel(newByteCode.get(i))) {
                additionalBytes++;
            }
            if (newByteCode.get(i).contains("(")) {
                additionalBytes += 2; // If we reach INVOKESTATIC, add the two additional Bytes
            }
            if (newByteCode.get(i).contains("[")) {
                additionalBytes += 2; // If we reach GETSTATIC or PUTSTATIC, add the two additional Bytes
            }
        }

        ArrayList<String> first = new ArrayList<>(newByteCode.subList(0, indexOfStart + 1));
        ArrayList<String> second = new ArrayList<>(newByteCode.subList(indexOfStart + 1, newByteCode.size()));

        first.add(String.format("%1$04X", indexOfEnd - indexOfStart + 2 + additionalBytes + (newByteCode.contains(currentLabel + "ELSESTART") ? 3 : 0)));

        newByteCode = new ArrayList<>(first);
        newByteCode.addAll(second);

        newByteCode.remove(currentLabel + "IFSTART");
        newByteCode.remove(currentLabel + "IFEND");
        newByteCode.remove(currentLabel + "ELSESTART");
        newByteCode.remove(currentLabel + "ELSEEND");

        return newByteCode;
    }

    public ArrayList<String> calculateJumpBytesForWHILE(ArrayList<String> byteCode, String currentLabel) {
        ArrayList<String> newByteCode = new ArrayList<>(byteCode);

        int indexOfStart = newByteCode.indexOf(currentLabel + "WHILESTART");
        int indexOfEnd = newByteCode.indexOf(currentLabel + "WHILEEND");

        int additionalBytes = 0;
        for (int i = newByteCode.indexOf(currentLabel + "WHILESTART"); i < newByteCode.indexOf(currentLabel + "WHILEEND"); i++) {
            if (newByteCode.get(i).length() == 4 && !checkIfLabel(newByteCode.get(i))) {
                additionalBytes++;
            }
            else if (newByteCode.get(i).contains("(")) {
                additionalBytes += 2; // If we reach INVOKESTATIC, add the two additional Bytes
            }
            else if (newByteCode.get(i).contains("[") ) {
                additionalBytes += 2; // If we reach GETSTATIC or PUTSTATIC, add the two additional Bytes
            }
        }

        ArrayList<String> first = new ArrayList<>(newByteCode.subList(0, indexOfStart + 1));
        ArrayList<String> second = new ArrayList<>(newByteCode.subList(indexOfStart + 1, newByteCode.size()));

        String hexByte = String.format("%1$04X", indexOfEnd - indexOfStart + 5 + additionalBytes);
        first.add(hexByte);

        int staticBytes = 0;
        if (first.get(first.size() - 4).contains("[")) {
            staticBytes++;
        }
        if (first.get(first.size() - 5).contains("[")) {
            staticBytes++;
        }
        else if (first.get(first.size() - 6).contains("[")) {
            staticBytes++;
        }

        String twosComplementHex = Integer.toHexString((~Integer.parseInt(hexByte, 16) - staticBytes) & 0xFFFF).toUpperCase();
        second.add(twosComplementHex);

        newByteCode = new ArrayList<>(first);
        newByteCode.addAll(second);

        newByteCode.remove(currentLabel + "WHILESTART");
        newByteCode.remove(currentLabel + "WHILEEND");

        return newByteCode;
    }

    private boolean checkIfLabel(String s) {
        return s.contains("START") || s.contains("END");
    }


    public final static String IF_ICMPEQ = "9f";
    public final static String IF_ICMPNE = "a0";
    public final static String IF_ICMPLT = "a1";
    public final static String IF_ICMPGE = "a2";
    public final static String IF_ICMPGT = "a3";
    public final static String IF_ICMPLE = "a4";
}
