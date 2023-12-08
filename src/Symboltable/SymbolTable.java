package Symboltable;

import Exceptions.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Map.Entry<String, Integer>> m_symbolTable = new HashMap<>(); // Identifier -> { (VAR x NAT) U (CONST x INT) }

    private final ArrayList<String> m_parameterList = new ArrayList<>(); // Parameter names

    private int m_addressCounter = 1; // Address for variable or parameter

    private int m_argCount = 0; // Number of parameters for functions

    private boolean m_isActive = false; // Always one symbol table active at all times

    private int m_currentLabelNumberIf = 1;
    private int m_currentLabelNumberWhile = 1;

    private final ArrayList<String> m_labelNumbersIf = new ArrayList<>();
    private final ArrayList<String> m_labelNumbersWhile = new ArrayList<>();

    public int callArgCount = 0; // Actual parameter count when calling the function

    public boolean isMain = false;

    public boolean isProcedure = false;

    public ArrayList<String> byteCode = new ArrayList<>();

    public ArrayList<Integer> callArgCounts = new ArrayList<>(); // Needed for nested, recursive calls

    public void reserveParameters(String id) throws SymbolAlreadyDefinedException {
        if (m_symbolTable.containsKey(id) || m_parameterList.contains(id))
            throw new SymbolAlreadyDefinedException("Symbol is already defined");

        m_parameterList.add(id);
    }

    public void addParameters() {
        m_addressCounter = isMain ? 1 : 0;
        for (String p : m_parameterList) {
            m_symbolTable.put(p, new AbstractMap.SimpleEntry<>("VAR",m_addressCounter++));
        }
    }

    public void addConstant(String id, Integer value) throws SymbolAlreadyDefinedException {
        if (m_symbolTable.containsKey(id))
            throw new SymbolAlreadyDefinedException("Symbol is already defined");

        m_symbolTable.put(id, new AbstractMap.SimpleEntry<>("CONST", value));
    }

    public void addVariable(String id) throws SymbolAlreadyDefinedException {
        if (m_symbolTable.containsKey(id))
            throw new SymbolAlreadyDefinedException("Symbol is already defined");

        m_symbolTable.put(id, new AbstractMap.SimpleEntry<>("VAR", m_addressCounter++));
    }

    public int getSymbol(String id) throws UnknownSymbolException {
        if (!m_symbolTable.containsKey(id))
            throw new UnknownSymbolException("Symbol is not defined");

        return m_symbolTable.get(id).getValue();
    }

    public int getCurrentAddress() {
        return m_addressCounter;
    }

    public boolean checkIfVariable(String id) {
        if (!m_symbolTable.containsKey(id))
            return false;

        return m_symbolTable.get(id).getKey().equals("VAR");
    }

    public boolean checkIfConstant(String id) {
        if (!m_symbolTable.containsKey(id))
            return false;

        return m_symbolTable.get(id).getKey().equals("CONST");
    }

    public boolean checkIfSymbol(String id) {
        return m_symbolTable.containsKey(id);
    }

    public void addLabelIf(String label) {
        m_labelNumbersIf.add(label);
    }

    public String getLabelIf(int index) {
        return m_labelNumbersIf.get(index);
    }

    public void removeLabelIf(int index) {
        m_labelNumbersIf.remove(index);
    }

    public void addLabelWhile(String label) {
        m_labelNumbersWhile.add(label);
    }

    public String getLabelWhile(int index) {
        return m_labelNumbersWhile.get(index);
    }

    public void removeLabelWhile(int index) {
        m_labelNumbersWhile.remove(index);
    }

    public int getVariableAddress(String id) {
        return m_symbolTable.get(id).getValue();
    }

    public int getArgCount() {
        return m_argCount;
    }

    public void incrementArgCount() {
        m_argCount++;
    }

    public boolean getIsActive() {
        return m_isActive;
    }

    public void setIsActive(boolean a) {
        m_isActive = a;
    }

    public int getCurrentLabelNumberIf() { return m_currentLabelNumberIf; }

    public int getCurrentLabelNumberWhile() { return m_currentLabelNumberWhile; }

    public void incrementCurrentLabelNumberIf() { m_currentLabelNumberIf++; }

    public void incrementCurrentLabelNumberWhile() { m_currentLabelNumberWhile++; }

    public ArrayList<String> getLabelNumbersIf() { return m_labelNumbersIf; }

    public ArrayList<String> getLabelNumbersWhile() { return m_labelNumbersWhile; }
}
