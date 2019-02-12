package com.ve.lexer;

import com.ve.view.text.document.CommonLanguage;

import java.util.ArrayList;
import java.util.HashMap;


public abstract class Language extends CommonLanguage {



    private final static char[] BASIC_C_OPERATORS = {
            '(', ')', '{', '}', '.', ',', ';', '=', '+', '-',
            '/', '*', '&', '!', '|', ':', '[', ']', '<', '>',
            '?', '~', '%', '^'
    };


    protected HashMap<String, Integer> _keywords = new HashMap<String, Integer>(0);
    protected HashMap<String, Integer> _names = new HashMap<String, Integer>(0);
    protected HashMap<String, String[]> _bases = new HashMap<String, String[]>(0);
    protected HashMap<String, Integer> _users = new HashMap<String, Integer>(0);
    protected HashMap<Character, Integer> _operators = generateOperators(BASIC_C_OPERATORS);

    private ArrayList<String> _ueserCache = new ArrayList<String>();
    private String[] _userWords = new String[0];
    private String[] _keyword;
    private String[] _name;

    public void updateUserWord() {
        String[] uw = new String[_ueserCache.size()];
        _userWords = _ueserCache.toArray(uw);
    }

    public String[] getUserWord() {
        return _userWords;
    }

    public String[] getNames() {
        return _name;
    }

    public String[] getBasePackage(String name) {
        return _bases.get(name);
    }

    public String[] getKeywords() {
        return _keyword;
    }

    public void setKeywords(String[] keywords) {
        _keyword = keywords;
        _keywords = new HashMap<String, Integer>(keywords.length);
        for (int i = 0; i < keywords.length; ++i) {
            _keywords.put(keywords[i], Lexer.KEYWORD);
        }
    }

    public void setNames(String[] names) {
        _name = names;
        ArrayList<String> buf = new ArrayList<String>();
        _names = new HashMap<String, Integer>(names.length);
        for (int i = 0; i < names.length; ++i) {
            if (!buf.contains(names[i]))
                buf.add(names[i]);
            _names.put(names[i], Lexer.NAME);
        }
        _name = new String[buf.size()];
        buf.toArray(_name);
    }

    public void addBasePackage(String name, String[] names) {
        _bases.put(name, names);
    }

    public void clearUserWord() {
        _ueserCache.clear();
        _users.clear();
    }

    public void addUserWord(String name) {
        if (!_ueserCache.contains(name) && !_names.containsKey(name))
            _ueserCache.add(name);
        _users.put(name, Lexer.NAME);
    }

    protected void setOperators(char[] operators) {
        _operators = generateOperators(operators);
    }

    private HashMap<Character, Integer> generateOperators(char[] operators) {
        HashMap<Character, Integer> operatorsMap = new HashMap<Character, Integer>(operators.length);
        for (int i = 0; i < operators.length; ++i) {
            operatorsMap.put(operators[i], Lexer.OPERATOR);
        }
        return operatorsMap;
    }

    public final boolean isOperator(char c) {
        return _operators.containsKey(Character.valueOf(c));
    }

    public final boolean isKeyword(String s) {
        return _keywords.containsKey(s);
    }

    public final boolean isName(String s) {
        return _names.containsKey(s);
    }

    public final boolean isBasePackage(String s) {
        return _bases.containsKey(s);
    }

    public final boolean isBaseWord(String p, String s) {
        String[] pkg = _bases.get(p);
        for (String n : pkg) {
            if (n.equals(s))
                return true;
        }
        return false;
    }

    public final boolean isUserWord(String s) {
        return _users.containsKey(s);
    }

    private boolean contains(String[] a, String s) {
        for (String n : a) {
            if (n.equals(s))
                return true;
        }
        return false;
    }

    private boolean contains(ArrayList<String> a, String s) {
        for (String n : a) {
            if (n.equals(s))
                return true;
        }
        return false;
    }



    public boolean isSentenceTerminator(char c) {
        return (c == '.');
    }

    public boolean isEscapeChar(char c) {
        return (c == '\\');
    }

    public boolean isProgLang() {
        return true;
    }

    public boolean isWordStart(char c) {
        return false;
    }

    public boolean isDelimiterA(char c) {
        return (c == '"');
    }


    public boolean isDelimiterB(char c) {
        return (c == '\'');
    }


    public boolean isLineAStart(char c) {
        return (c == '#');
    }


    public boolean isLineBStart(char c) {
        return false;
    }

    public boolean isLineStart(char c0, char c1) {
        return (c0 == '/' && c1 == '/');
    }

    public boolean isMultilineStartDelimiter(char c0, char c1) {
        return (c0 == '/' && c1 == '*');
    }


    public boolean isMultilineEndDelimiter(char c0, char c1) {
        return (c0 == '*' && c1 == '/');
    }
}
