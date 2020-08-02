package Parse;


import Files.Configurations;
import Files.CorpusDocument;
import Stemmer.Stemmer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {

    private static HashSet<String> h_stopWords = new HashSet<>();
    private static boolean isStemming;

    private HashMap<String, LinkedHashMap<CorpusDocument, Integer>> entitiesMap;
    private HashMap<String, LinkedHashMap<CorpusDocument, Integer>> corpusTerms;
    private LinkedHashMap<CorpusDocument, LinkedHashMap<String, Integer>> docsMap;

    //with 31 days
    private enum longMonths {
        January("01"),
        JANUARY("01"),
        Jan("01"),
        March("03"),
        MARCH("03"),
        Mar("03"),
        May("05"),
        MAY("05"),
        July("07"),
        JULY("07"),
        Jul("07"),
        August("08"),
        AUGUST("08"),
        Aug("08"),
        October("10"),
        OCTOBER("10"),
        Oct("10"),
        December("12"),
        DECEMBER("12"),
        Dec("12");
        private String monthValue;

        private longMonths(String value) {
            this.monthValue = value;
        }

        private String getMonthValue() {
            return monthValue;
        }
    }

    //with 30 days
    enum shortMonths {
        April("04"),
        APRIL("04"),
        Apr("04"),
        June("06"),
        JUNE("06"),
        Jun("06"),
        September("09"),
        SEPTEMBER("09"),
        Sep("09"),
        November("11"),
        NOVEMBER("11"),
        Nov("11");
        private String monthValue;

        private shortMonths(String value) {
            this.monthValue = value;
        }

        private String getMonthValue() {
            return this.monthValue;
        }
    }

    //with 28-29 days
    enum february {
        February("02"),
        FEBRUARY("02"),
        Feb("02");
        private String monthValue;

        private february(String value) {
            this.monthValue = value;
        }

        private String getMonthValue() {
            return this.monthValue;
        }
    }


    public Parse() {
        corpusTerms = new HashMap<>();
        entitiesMap = new HashMap<>();
        docsMap = new LinkedHashMap<>();
    }

    /**
     * resets all of the structures in this class
     */
    public void resetParse() {
        corpusTerms.clear();
        entitiesMap.clear();
        docsMap.clear();
    }

    /**
     * creates stop words list from the stop words path = corpus path
     * @param pathStopWord
     */
    public static void createStopWordList(String pathStopWord) {
        if (h_stopWords.size() == 0) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(pathStopWord));
                String line;
                while ((line = br.readLine()) != null) {
                    h_stopWords.add(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * sets the stemming option once in the beginning
     */
    public static void setStemming(){
        isStemming = Configurations.getStemmingProp();
    }

    /**
     * checks if some word is a stop word
     * @param token
     * @return
     */
    private boolean isStopWord(String token) {
        return h_stopWords.contains(token.toLowerCase());
    }

    /**
     * gets list of tokens
     * @param doc documents to tokenize
     * @return list of tokens
     */
    private PTBTokenizer getIterator(CorpusDocument doc) {
        if (doc != null) {
            //tokenization with stanfordNLP tokenizer
            PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(new StringReader(doc.getText()), new CoreLabelTokenFactory(), "ptb3Escaping=false,untokenizable=noneKeep,normalizeCurrency=false");
            return tokenizer;
        }
        return null;
    }

    /**
     * parse the documents according to the required laws
     * @param doc to parse
     */
    public void parsing(CorpusDocument doc) {
        if (doc != null) {
            PTBTokenizer<CoreLabel> tokenizer = getIterator(doc);
            List<CoreLabel> l = tokenizer.tokenize();
            //removes summary
            if (l.get(0).toString().toLowerCase().equals("summary"))
                l.remove(0);
            orWordParse(l,doc);
            timeParse(l,doc);
            percentParse(l, doc);
            pricesParse(l, doc);
            datesParse(l, doc);
            rangesPhrasesParse(l, doc);
            numberParse(l, doc);
            upperLowerLettersParse(l, doc);
            namesEntitiesParser(l, doc);
        }
    }

    /**
     * parse the documents according to the numbers laws
     * @param tokenList
     * @param doc
     */
    private void numberParse(List<CoreLabel> tokenList, CorpusDocument doc) {

        String tempTerm, tempText, tempNextText;
        tempTerm = tempText = tempNextText = "";
        double doubleValueOfString;
        //Regular expression: numbers under thousand with or without dots
        String regexUnderThousand = "^(([1-9][0-9]{0,2})|0)(\\.[0-9]+)?$";
        //Regular expression: Fractions
        String regexFractionNumber = "^[1-9][0-9]*/[1-9][0-9]*$";
        //Regular expression: numbers over thousand with or without commas and perhaps with numbers after dot
        String regexOverThousand = "^(([1-9][0-9]{3,5})|([1-9][0-9]?[0-9]?,[0-9]{3}))(.[0-9]+)?$";
        //Regular expression: numbers over million with or without commas and perhaps with numbers after dot
        String regexOverMillion = "^(([1-9][0-9]{6,8})|([1-9][0-9]?[0-9]?(,[0-9]{3}){2}))(.[0-9]+)?$";
        //Regular expression: numbers over billion with or without commas and perhaps with numbers after dot
        String regexOverBillion = "^(([1-9][0-9]{9,})|([1-9][0-9]?[0-9]?(,[0-9]{3}){3}))(.[0-9]+)?$";

        //Pattern compiler for each regular expression
        Pattern patternFraction = Pattern.compile(regexFractionNumber);
        Pattern patternUnderT = Pattern.compile(regexUnderThousand);
        Pattern patternOverT = Pattern.compile(regexOverThousand);
        Pattern patternOverM = Pattern.compile(regexOverMillion);
        Pattern patternOverB = Pattern.compile(regexOverBillion);

        //only three numbers after dot
        DecimalFormat df = new DecimalFormat("###.###");

        try {
            for (int i = 0; i < tokenList.size(); i++) {
                tempText = tokenList.get(i).toString();
                if (i < tokenList.size() - 1)
                    tempNextText = tokenList.get(i + 1).toString();
                //dont insert numbers with % and $
                if (tempText.equals("$") || tempNextText.equals("%") || tempNextText.equals("percent") || tempNextText.equals("percentage")) {
                    ++i;
                    continue;
                }
                Matcher matcherFraction = patternFraction.matcher(tempText);
                Matcher matcherUnderT = patternUnderT.matcher(tempText);
                Matcher matcherOverT = patternOverT.matcher(tempText);
                Matcher matcherOverM = patternOverM.matcher(tempText);
                Matcher matcherOverB = patternOverB.matcher(tempText);


                //Billions
                if (matcherOverB.matches()) {
                    //integer value of the current string
                    doubleValueOfString = NumberFormat.getNumberInstance(Locale.UK).parse(tempText).doubleValue();
                    tempTerm = df.format(doubleValueOfString / 1000000000.0) + "B";
                    addTermToMap(tempTerm, doc);
                }
                //Millions
                else if (matcherOverM.matches()) {
                    //integer value of the current string
                    doubleValueOfString = NumberFormat.getNumberInstance(Locale.UK).parse(tempText).doubleValue();
                    tempTerm = df.format(doubleValueOfString / 1000000.0) + "M";
                    addTermToMap(tempTerm, doc);
                }
                //Thousands
                else if (matcherOverT.matches()) {
                    //integer value of the current string
                    doubleValueOfString = NumberFormat.getNumberInstance(Locale.UK).parse(tempText).doubleValue();
                    tempTerm = df.format(doubleValueOfString / 1000.0) + "K";
                    addTermToMap(tempTerm, doc);
                }
                //Under Thousand
                else if (matcherUnderT.matches()) {
                    if (!tempNextText.equals("")) {
                        //fraction
                        Matcher nextMatcherFraction = patternFraction.matcher(tempNextText);
                        if (matcherFraction.matches())
                            tempTerm = tempText + " " + tempNextText;
                            //double number
                        else {
                            tempTerm = df.format(Double.parseDouble(tempText));
                            tempTerm = getMatchNumber(tempTerm, tempNextText);
                        }
                        addTermToMap(tempTerm, doc);
                        ++i;
                    }
                    //last word in the text
                    else {
                        tempTerm = df.format(Double.parseDouble(tempText));
                        addTermToMap(tempTerm, doc);
                    }
                }
                //only fraction
                else if (matcherFraction.matches()) {
                    if (!tempNextText.equals("")) {
                        tempTerm = tempText;
                        tempTerm = getMatchNumber(tempTerm, tempNextText);
                        addTermToMap(tempTerm, doc);
                        ++i;
                    } else
                        addTermToMap(tempText, doc);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * checks if number is Million, Thousand, or Billion
     * @param tempTerm
     * @param tempNextText
     * @return
     */
    private String getMatchNumber(String tempTerm, String tempNextText) {
        switch (tempNextText) {
            case "Billion":
                tempTerm += "B";
                break;

            case "Million":
                tempTerm += "M";
                break;

            case "Thousand":
                tempTerm += "K";
                break;

            default:
                break;
        }
        return tempTerm;
    }

    /**
     * parse the documents according to the percent laws
     * @param tokenList
     * @param doc
     */
    private void percentParse(List<CoreLabel> tempText, CorpusDocument doc) {
        String tempTerm, tempNextTerm;
        tempTerm = tempNextTerm = "";
        String regexWPercent = "^-?\\d+((\\.|\\/|\\-|\\,)\\d*)?$|^e$|%$|^n$";
        Pattern patternW = Pattern.compile(regexWPercent);

        for (int i = 0; i < tempText.size(); i++) {
            tempTerm = tempText.get(i).toString();
            Matcher matcherW = patternW.matcher(tempTerm);
            if (matcherW.matches()) {
                if ((tempTerm.length() > 1 && tempTerm.contains("%")) || tempTerm.contains("e"))
                    addTermToMap(tempTerm, doc);
                else if (i < tempText.size() - 1) {
                    tempNextTerm = tempText.get(i + 1).toString();
                    if (tempNextTerm.equals("%") || tempNextTerm.equals("percent") || tempNextTerm.equals("percentage")) {
                        tempTerm += "%";
                        addTermToMap(tempTerm, doc);
                        ++i;
                    }
                }
            }
        }
    }

    /**
     * parse the documents according to the prices laws
     * @param tokenList
     * @param doc
     */
    private void pricesParse(List<CoreLabel> tokenList, CorpusDocument doc) {
        String price = "";
        double doubleValueOfString = 0;
        String tempText, tempNextText;
        tempText = tempNextText = "";
        //Regular expression: numbers under million with or without dots, fractions and commas.
        String regexUnderM = "(^\\$?(\\d\\d{0,5}|0)((\\.\\d+)|(\\s[1-9]\\d*\\/[1-9]\\d*))?|^\\$?(\\d\\d?\\d?)(,\\d{3})?)(m|bn)?$";
        //Regular expression: numbers over million with or without dots, fractions, commas, and checks if has "bn" or "m".
        String regexOverM = "^\\$?\\d\\d{6,}((\\.[0-9]+)|(\\s[1-9]\\d*\\/[1-9]\\d*))?$|^\\$?(\\d\\d?\\d?)(,\\d{3}){2,}$";
        //Regular expression: fractions
        String regexFraction = "^[1-9][0-9]*\\/[1-9][0-9]*$";

        Pattern patternU = Pattern.compile(regexUnderM);
        Pattern patternO = Pattern.compile(regexOverM);
        Pattern patternF = Pattern.compile(regexFraction);

        for (int i = 0; i < tokenList.size(); i++) {
            tempText = tokenList.get(i).toString();
            if (i < tokenList.size() - 1)
                tempNextText = tokenList.get(i + 1).toString();
            Matcher matchU = patternU.matcher(tempText);
            Matcher matchO = patternO.matcher(tempText);
            //number under million
            if (matchU.matches()) {
                if (tempText.contains("$") || (i > 0 && tokenList.get(i - 1).toString().equals("$"))) {
                    price = tempText.replace("$", "");
                    //price types with $ (million,billion,price).
                    if (i < tokenList.size() - 1) {
                        switch (tempNextText) {
                            case "million":
                                price += " M Dollars";
                                break;
                            case "billion":
                                price += "000 M Dollars";
                                break;
                            default:
                                price += " Dollars";
                                break;
                        }
                    }
                    //last word is price with $
                    else
                        price += " Dollars";
                    //h_corpusTerms.put(price, new Term(price, doc));
                }
                //price under million with fraction or without.
                else if (!tempNextText.equals("")) {
                    if (tempNextText.equals("Dollars") || tempNextText.equals("m") || tempNextText.equals("bn")) {
                        if (tempText.contains("m") || ((tempNextText.equals("m")) && i < tokenList.size() - 2 && tokenList.get(i + 2).toString().equals("Dollars"))) {
                            price = tempText.replaceAll("m", "") + " M Dollars";
                        } else if (tempText.contains("bn") || ((tempNextText.equals("bn")) && i < tokenList.size() - 2 && tokenList.get(i + 2).toString().equals("Dollars"))) {
                            price = tempText.replaceAll("bn", "") + "000 M Dollars";
                        } else if (tempNextText.equals("Dollars")) {
                            price = tempText + " Dollars";
                        }
                        //h_corpusTerms.put(price, new Term(price, doc));
                    }
                    //price with fraction
                    else if (patternF.matcher(tempNextText).matches() && i < tokenList.size() - 2 && tokenList.get(i + 2).toString().equals("Dollars")) {
                        price = tempText + tempNextText + " Dollars";
                        //h_corpusTerms.put(price, new Term(price, doc));
                    }
                    //price from this format: "price million/billion U.S. dollars"
                    else if (i < tokenList.size() - 3 && tokenList.get(i + 3).toString().equals("dollars") && tokenList.get(i + 2).toString().equals("U.S.")) {
                        if (tempNextText.equals("million")) {
                            price = tempText + " M Dollars";
                        } else if (tempNextText.equals("billion")) {
                            price = tempText + "000 M Dollars";
                            //h_corpusTerms.put(price, new Term(price, doc));
                        }
                    }
                }
            }
            //price over million
            else if (matchO.matches()) {
                try {
                    //price with $
                    if (tempText.contains("$") || (i > 0 && tokenList.get(i - 1).toString().equals("$"))) {
                        price = tempText.replace("$", "");
                        doubleValueOfString = NumberFormat.getNumberInstance(Locale.UK).parse(price).doubleValue() / 1000000.0;
                        price = String.valueOf(doubleValueOfString) + " M Dollars";
                    }
                    //price with "Dollars" after it.
                    else if (i < tokenList.size() - 1 && tempNextText.equals("Dollars")) {
                        doubleValueOfString = NumberFormat.getNumberInstance(Locale.UK).parse(tempText).doubleValue() / 1000000.0;
                        price = String.valueOf(doubleValueOfString) + " M Dollars";
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (price != "") {
                addTermToMap(price, doc);
                price = "";
            }
        }
    }

    /**
     * parse the documents according to the dates laws
     * @param tokenList
     * @param doc
     */
    private void datesParse(List<CoreLabel> tokenList, CorpusDocument doc) {
        String tempToken, tempText, tempTextNext;
        tempText = tempToken = tempTextNext = "";

        //day 1-31
        String regexDays = "^([1-9]|[1-2][0-9]|3[0-1])$";
        //year from 0-2999
        String regexYear = "^([0-2][0-9]{3})$";

        Pattern daysPattern = Pattern.compile(regexDays);
        Pattern yearsPattern = Pattern.compile(regexYear);

        for (int i = 0; i < tokenList.size(); i++) {
            tempText = tokenList.get(i).toString();
            if (i < tokenList.size() - 1)
                tempTextNext = tokenList.get(i + 1).toString();
            else
                tempTextNext = "";
            //must have at least day and month
            if (!tempTextNext.equals("")) {
                Matcher daysMatcher = daysPattern.matcher(tempText);
                Matcher yearMatcher = yearsPattern.matcher(tempTextNext);
                //checks if day before month
                if (daysMatcher.matches() && isMonth(tempTextNext, Integer.parseInt(tempText))) {
                    if (isLongMonth(tempTextNext)) {
                        tempToken = longMonths.valueOf(tempTextNext).getMonthValue();
                    } else if (isShortMonth(tempTextNext)) {
                        tempToken = shortMonths.valueOf(tempTextNext).getMonthValue();
                    } else if (isFebMonth(tempTextNext)) {
                        tempToken = february.valueOf(tempTextNext).getMonthValue();
                    }
                    //adds zero to 1-9 -> 01-09
                    if (Integer.parseInt(tempText) < 10)
                        tempToken += "-0" + tempText;
                    else
                        tempToken += "-" + tempText;
                    addTermToMap(tempToken, doc);
                    ++i;
                }
                //checks if month before day
                else if (daysPattern.matcher(tempTextNext).matches() && isMonth(tempText, Integer.parseInt(tempTextNext))) {
                    if (isLongMonth(tempText)) {
                        tempToken = longMonths.valueOf(tempText).getMonthValue();
                    } else if (isShortMonth(tempText)) {
                        tempToken = shortMonths.valueOf(tempText).getMonthValue();
                    } else if (isFebMonth(tempText)) {
                        tempToken = february.valueOf(tempText).getMonthValue();
                    }
                    //adds zero to 1-9 -> 01-09
                    if (Integer.parseInt(tempTextNext) < 10)
                        tempToken += "-0" + tempTextNext;
                    else
                        tempToken += "-" + tempTextNext;
                    addTermToMap(tempToken, doc);
                    ++i;
                }
                //checks if month before year , sends 0 to isMonth function because it can't be a month situation because of the regular exp
                else if (yearMatcher.matches() && (isMonth(tempText, 0) || (i > 0 && tempText.equals(",") && isMonth(tokenList.get(i - 1).toString(), 0)))) {
                    if (tempText.equals(","))
                        tempText = tokenList.get(i - 1).toString();
                    if (isLongMonth(tempText)) {
                        tempToken = tempTextNext + "-" + longMonths.valueOf(tempText).getMonthValue();
                    } else if (isShortMonth(tempText)) {
                        tempToken = tempTextNext + "-" + shortMonths.valueOf(tempText).getMonthValue();
                    } else if (isFebMonth(tempText)) {
                        tempToken = tempTextNext + "-" + february.valueOf(tempText).getMonthValue();
                    }
                    addTermToMap(tempToken, doc);
                    ++i;
                }
            }
        }
    }

    /**
     * checks if the token is a legal month with 28-31 days
     * @param token
     * @param day
     * @return
     */
    private boolean isMonth(String token, int day) {
        if (day > 0)
            return isLongMonth(token) || (isShortMonth(token) && day <= 30) || (isFebMonth(token) && day <= 29);
        else
            return isLongMonth(token) || isShortMonth(token) || isFebMonth(token);

    }

    /**
     * chekcs if the given month is a long month with 31 days
     * @param token
     * @return true if the token is long month
     */
    private boolean isLongMonth(String token) {
        for (longMonths month : longMonths.values()) {
            if (token.equals(month.name()))
                return true;
        }
        return false;
    }

    /**
     * chekcs if the given month is a short month with 30 days
     * @param token
     * @return true if the token is short month
     */
    private boolean isShortMonth(String token) {
        for (shortMonths month : shortMonths.values()) {
            if (token.equals(month.name()))
                return true;
        }
        return false;
    }

    /**
     * chekcs if the given month is a february month with 28 days
     * @param token
     * @return true if the token is feb month
     */
    private boolean isFebMonth(String token) {
        for (february month : february.values()) {
            if (token.equals(month.name()))
                return true;
        }
        return false;
    }

    /**
     * parse the documents according to the ranges laws
     * @param tokenList
     * @param doc
     */
    private void rangesPhrasesParse(List<CoreLabel> tokenList, CorpusDocument doc) {
        String tempText = "";
        String validNumbers = "((\\d+)|(([1-9]\\d?\\d?)(,[0-9]{3})+))((\\.|\\/)\\d+)?";
        String validWords = "[a-zA-Z]+";
        String regexWords = "^[a-zA-Z]+-[a-zA-Z]+(-[a-zA-Z]+)?$";
        String regexNumbers = "^" + validNumbers + "-" + validNumbers + "$";
        String regexNumbersWords = "^" + validWords + "-" + validNumbers + "|" + validNumbers + "-" + validWords + "$";

        Pattern twoOrThreeWordsPattern = Pattern.compile(regexWords);
        Pattern numberWordPattern = Pattern.compile(regexNumbersWords);
        Pattern numbersPattern = Pattern.compile(regexNumbers);
        Pattern singleNumberPattern = Pattern.compile("^" + validNumbers + "$");

        for (int i = 0; i < tokenList.size(); i++) {

            tempText = tokenList.get(i).toString();
            Matcher twoThreeWordsMatcher = twoOrThreeWordsPattern.matcher(tempText);
            Matcher numberWordMatcher = numberWordPattern.matcher(tempText);
            Matcher numbersMatcher = numbersPattern.matcher(tempText);

            //checks for terms with '-'
            if (twoThreeWordsMatcher.matches() || numbersMatcher.matches() || numberWordMatcher.matches()) {
                addTermToMap(tempText.toLowerCase(), doc);
                /*String[] numbersRange = tempText.split("-");
                for (String token : numbersRange) {
                    if (token.matches("^" + validNumbers + "$") || token.matches("^" + validWords + "$"))
                        addTermToMap(new Term(token), doc);
                }*/
            }

            //checks for terms according to this format: "between *NUM* and *NUM*"
            else if (i < tokenList.size() - 3 && (tempText.equals("between") || tempText.equals("Between")) && tokenList.get(i + 2).toString().equals("and")) {
                Matcher firstNumber = singleNumberPattern.matcher(tokenList.get(i + 1).toString());
                Matcher secondNumber = singleNumberPattern.matcher(tokenList.get(i + 3).toString());
                if (firstNumber.matches() && secondNumber.matches()) {
                    String tempTerm = String.format("%s-%s", tokenList.get(i + 1).toString(), tokenList.get(i + 3).toString());
                    addTermToMap(tempTerm.toLowerCase(), doc);
                }
            }
        }
    }

    /**
     * parse the documents according to the UpperLower laws
     * @param tokenList
     * @param doc
     */
    private void upperLowerLettersParse(List<CoreLabel> tokensList, CorpusDocument doc) {

        String tempToken;
        String tokenUpper, tokenLower;
        tokenLower = tokenUpper = "";
        tempToken = "";
        for (int i = 0; i < tokensList.size(); i++) {
            tempToken = tokensList.get(i).toString();
            //its a word
            if (isWord(tempToken)) {
                if (isStemming) {
                    Stemmer stemmer = new Stemmer();
                    tokenLower = stemmer.stem(tempToken.toLowerCase());
                    tokenUpper = tokenLower.toUpperCase();
                } else {
                    tokenUpper = tempToken.toUpperCase();
                    tokenLower = tempToken.toLowerCase();
                }

                if (isUpperCase(tempToken)) {
                    if (!corpusTerms.containsKey(tokenLower)) {
                        addTermToMap(tokenUpper, doc);
                    } else
                        addTermToMap(tokenLower, doc);
                } else {
                    if (corpusTerms.containsKey(tokenUpper)) {
                        LinkedHashMap<CorpusDocument, Integer> termFreqUpperCase = corpusTerms.get(tokenUpper);
                        if (!corpusTerms.containsKey(tokenLower)) {
                            addTermToMap(tokenLower, doc);
                        }
                        mergeTermsInDoc(tokenLower, tokenUpper, doc);
                        LinkedHashMap<CorpusDocument, Integer> termFreqLowerCase = corpusTerms.get(tokenLower);
                        corpusTerms.replace(tokenLower, mergeDocList(termFreqLowerCase, termFreqUpperCase));
                        corpusTerms.remove(tokenUpper);
                    } else {
                        addTermToMap(tokenLower, doc);
                    }
                }
            }
        }
    }

    /**
     * merges two docs list of the same term
     * @param firstList
     * @param secondList
     * @return
     */
    private LinkedHashMap<CorpusDocument, Integer> mergeDocList(LinkedHashMap<CorpusDocument, Integer> firstList, LinkedHashMap<CorpusDocument, Integer> secondList) {
        for (Map.Entry<CorpusDocument, Integer> document : secondList.entrySet()) {
            if (firstList.containsKey(document.getKey())) {
                firstList.replace(document.getKey(), document.getValue() + firstList.get(document.getKey()));
            } else {
                firstList.put(document.getKey(), document.getValue());
            }
        }
        return firstList;
    }

    /**
     * checks if the given token is a word
     * @param token
     * @return
     */
    private boolean isWord(String token) {
        if (!token.equals("") && ((token.charAt(0) >= 'a' && token.charAt(0) <= 'z') || (token.charAt(0) >= 'A' && token.charAt(0) <= 'Z')) && !isMonth(token, 0) && !token.contains("/") && !isStopWord(token.toLowerCase())) {
            return true;
        }
        return false;
    }

    /**
     * checks if the given word is an upper case word
     * @param token
     * @return
     */
    private boolean isUpperCase(String token) {
        return token.charAt(0) >= 'A' && token.charAt(0) <= 'Z';
    }

    /**
     * parse the documents according to the entities laws - entity = serie of two or more words that start with upper case letter
     * @param tokenList
     * @param doc
     */
    private void namesEntitiesParser(List<CoreLabel> tokenList, CorpusDocument doc) {
        String entity = "";
        for (int i = 0; i < tokenList.size(); i++) {
            while (i < tokenList.size() && !tokenList.get(i).toString().equals("") && !isStopWord(tokenList.get(i).toString().toLowerCase()) && isUpperCase(tokenList.get(i).toString())) {
                entity += tokenList.get(i).toString() + " ";
                ++i;
            }
            if (!entity.equals("")) {
                if (entity.split(" ").length > 1) {
                    entity = entity.trim().toUpperCase(); // remove last whitespace
                    //Term newEntity = new Term(entity.toUpperCase()); // entities in upper case according to ass 2
                    if (entitiesMap.containsKey(entity)) {
                        if (entitiesMap.get(entity).containsKey(doc))
                            entitiesMap.get(entity).replace(doc, entitiesMap.get(entity).get(doc) + 1);
                        else
                            entitiesMap.get(entity).put(doc, 1);
                    } else {
                        LinkedHashMap<CorpusDocument, Integer> documentList = new LinkedHashMap<>();
                        documentList.put(doc, 1);
                        entitiesMap.put(entity, documentList);
                    }
                }
                entity = "";
            }
        }
    }

    /**
     * parse the documents according to the or law - **NEW LAW** - separates word with / to two separate words
     * @param tokenList
     * @param doc
     */
    private void orWordParse(List<CoreLabel> tokensList, CorpusDocument doc){
        String[] orWords;
        String tempToken;
        String regexOrWord = "^[a-zA-Z]+\\/[a-zA-Z+]$";
        Pattern wordPattern = Pattern.compile(regexOrWord);
        for (int i=0;i<tokensList.size();i++){
            tempToken = tokensList.get(i).toString();
            Matcher match = wordPattern.matcher(tempToken);
            if (match.matches()){
                orWords = tempToken.split("\\/");
                for (String term : orWords){
                    if (isStemming) {
                        Stemmer stemmer = new Stemmer();
                        term = stemmer.stem(tempToken.toLowerCase());
                    }
                    addTermToMap(term,doc);
                }
            }
        }
    }

    /**
     * parse the documents according to the Time law - **NEW LAW** - adds time terms. example: 1100 GMT
     * @param tokenList
     * @param doc
     */
    private void timeParse(List<CoreLabel> tokensList, CorpusDocument doc){
        String tempToken;
        String regexOrWord = "^[0-9]{4}$";
        Pattern wordPattern = Pattern.compile(regexOrWord);
        for (int i=0;i<tokensList.size()-1;i++){
            tempToken = tokensList.get(i).toString();
            Matcher match = wordPattern.matcher(tempToken);
            if (match.matches() && tokensList.get(i+1).toString().equals("GMT")){
                tempToken += " GMT";
                addTermToMap(tempToken,doc);
            }
        }
    }

    /**
     * returns the terms of the corpus - without entities
     * @return table of terms and and list of documents and the number of appearances of the term in the doc
     */
    public HashMap<String, LinkedHashMap<CorpusDocument, Integer>> getCorpusTerms() {
        return corpusTerms;
    }

    /**
     * returns the entities of the corpus
     * @return table of entities and and list of documents and the number of appearances of the entity in the doc
     */
    public HashMap<String, LinkedHashMap<CorpusDocument, Integer>> getEntitiesMap() {
        return entitiesMap;
    }

    /**
     * returns the documents map
     * @return table of documents  and list of terms and the number of appearances of the terms in the doc
     */
    public LinkedHashMap<CorpusDocument, LinkedHashMap<String, Integer>> getDocsMap() {
        return docsMap;
    }

    /**
     * adds term to the tables
     * @param term
     * @param doc
     */
    private void addTermToMap(String term, CorpusDocument doc) {
        if (term != null && !term.equals("") && !isStopWord(term)) {
            addTermToDoc(term, doc);
            if (corpusTerms.containsKey(term)) {
                if (corpusTerms.get(term).containsKey(doc)) {
                    corpusTerms.get(term).replace(doc, corpusTerms.get(term).get(doc) + 1);
                } else {
                    corpusTerms.get(term).put(doc, 1);
                }
            } else {
                LinkedHashMap<CorpusDocument, Integer> termFreq = new LinkedHashMap<>();
                termFreq.put(doc, 1);
                corpusTerms.put(term, termFreq);
            }
        }
    }

    /**
     * adds term to doc table
     * @param term
     * @param doc
     */
    private void addTermToDoc(String term, CorpusDocument doc) {
        if (docsMap.containsKey(doc)) {
            if (docsMap.get(doc).containsKey(term))
                docsMap.get(doc).replace(term, docsMap.get(doc).get(term) + 1);
            else
                docsMap.get(doc).put(term, 1);
        } else {
            docsMap.put(doc, new LinkedHashMap<>());
            docsMap.get(doc).put(term, 1);
        }
    }

    /**
     * merge two terms (both are equals) in the given document - sum their number of appearances
     * @param lowerCase
     * @param upperCase
     * @param doc
     */
    private void mergeTermsInDoc(String lowerCase, String upperCase, CorpusDocument doc) {
        if (docsMap.get(doc).containsKey(lowerCase) && docsMap.get(doc).containsKey(upperCase)) {
            docsMap.get(doc).replace(lowerCase, docsMap.get(doc).get(lowerCase) + docsMap.get(doc).get(upperCase));
            docsMap.get(doc).remove(upperCase);
        }
    }

}



