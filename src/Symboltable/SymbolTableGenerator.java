package Symboltable;

import Exceptions.UnknownFunctionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SymbolTableGenerator {
    private final Map<String, SymbolTable> m_symbolTableList = new HashMap<>(); // Function name -> SymbolTable

    public void addSymbolTable(String name) {
        m_symbolTableList.put(name, new SymbolTable());
    }

    public void incrementParameterCount(String name) {
        m_symbolTableList.get(name).incrementArgCount();
    }

    public Map<String, SymbolTable> getSymbolTableList() {
        return m_symbolTableList;
    }

    public SymbolTable getSymbolTable(String name) {
        if (!m_symbolTableList.containsKey(name)) {
            return null;
        }
        return m_symbolTableList.get(name);
    }

    public SymbolTable getActiveSymbolTable() {
        return m_symbolTableList.values().stream()
                .filter(SymbolTable::getIsActive)
                .findFirst()
                .orElse(null);
    }

    public void setSymbolTableActive(String name) {
        m_symbolTableList.get(name).setIsActive(true);
    }

    public void setCurrentActiveToInactive() {
        Optional<SymbolTable> firstActiveSymbolTable = m_symbolTableList.values().stream()
                .filter(SymbolTable::getIsActive)
                .findFirst();

        firstActiveSymbolTable.ifPresent(symbolTable -> symbolTable.setIsActive(false));
    }

    public boolean checkIfMethod(String name) {
        return m_symbolTableList.containsKey(name);
    }
}
