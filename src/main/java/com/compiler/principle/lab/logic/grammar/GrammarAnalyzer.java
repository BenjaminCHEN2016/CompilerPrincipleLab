package com.compiler.principle.lab.logic.grammar;

import org.springframework.util.ResourceUtils;

import java.util.*;

public class GrammarAnalyzer {

    private static String seperator = "======================================================================";

    public static GrammarResponse analyze(String sourceCode) {
        GrammarResponse ret = new GrammarResponse();
        System.out.println(seperator);
        System.out.println("Reading production rules...");
        ProductionFactory factory = new ProductionFactory();
        List<Production> productions;
        try {
            productions = factory.fromFile(ResourceUtils.getFile("classpath:grammar/grammar_rules.txt"));
        } catch (Exception e) {
            ret.setStatus("system error");
            ret.setError("Unable to load grammar rules.");
            return ret;
        }
        System.out.println("Productions read.");
        System.out.println("Generating parsing table...");
        LLParser parser = new LLParser(productions, factory.getStartSymbol());
        //System.out.println(parser.getMTable());
        System.out.println("Preparing parsing table json...");
        List<TerminalSymbol> terminalSymbols = new ArrayList<>(parser.getMTable().getTerminalSymbols());
        List<String> TSRet = new ArrayList<>();
        for (TerminalSymbol it : terminalSymbols) {
            TSRet.add(it.getToken().getType());
        }
        List<Symbol> symbols = new ArrayList<>(parser.getMTable().getNoTerminalSymbols());
        List<String> SRet = new ArrayList<>();
        for (Symbol it : symbols) {
            SRet.add(it.getName());
        }
        HashMap<String, HashMap<String, String>> PRet = new HashMap<>();
        for (Symbol it : symbols) {
            HashMap<String, String> TPTmp = new HashMap<>();
            for (TerminalSymbol tit : terminalSymbols) {
                ArrayList<Production> prods = parser.getMTable().get(it, tit);
                if(prods.size() != 1){
                    continue;
                }
                Production prod = prods.get(0);
                String rightSide = "";
                for (int i = 0; i < prod.getRight().size(); i++) {
                    rightSide = rightSide.concat(prod.getRight().get(i).getName().equals("NULL") ?
                            "ε" : prod.getRight().get(i).getName()) + " ";
                }
                TPTmp.put(tit.getToken().getType(), prod.getLeft().getName() + " → " + rightSide);
            }
            PRet.put(it.getName(), TPTmp);
        }
        System.out.println("Parsing table json done.");
        ret.setTerminalSymbols(TSRet);
        ret.setSymbols(SRet);
        ret.setParsingTable(PRet);
        System.out.println("Parsing table done.");
        boolean hasError = false;
        long startTime = System.currentTimeMillis();
        try {
            ret.setGrammarTree(parser.llParse(sourceCode));
        } catch (ParserException e) {
            hasError = true;
            if (e.getMessage().equals("Syntax error.")) {
                ret.setStatus("Syntax error.");
                ret.setErrorSymbol(e.getSymbol());
                ret.setErrorTokenItem(e.getTokenItem());
            } else {
                ret.setStatus("Lex error.");
                ret.setError(e.getMessage());
            }
        }
        long endTime = System.currentTimeMillis();
        if (!hasError) ret.setStatus(String.valueOf(endTime - startTime) + "ms");
        return ret;
    }
}
