package engine;


public class PorterStemmer {

    private char[] b;
    private int i, j, k, k0;
    private boolean dirty = false;
    private static final int INC = 200;

    public PorterStemmer() {
        b = new char[INC];
        i = 0;
    }

    public void add(char ch) {
        if (i == b.length) {
            char[] newb = new char[i + INC];
            System.arraycopy(b, 0, newb, 0, i);
            b = newb;
        }
        b[i++] = ch;
    }

    public void add(char[] w, int wLen) {
        if (i + wLen >= b.length) {
            char[] newb = new char[i + wLen + INC];
            System.arraycopy(b, 0, newb, 0, i);
            b = newb;
        }
        System.arraycopy(w, 0, b, i, wLen);
        i += wLen;
    }

    public String toString() {
        return new String(b, k0, k - k0 + 1);
    }

    public int getResultLength() {
        return k - k0 + 1;
    }

    public char[] getResultBuffer() {
        return b;
    }

    public int getResultOffset() {
        return k0;
    }

    private boolean cons(int i) {
        switch (b[i]) {
            case 'a': case 'e': case 'i': case 'o': case 'u': return false;
            case 'y': return (i == k0) ? true : !cons(i - 1);
            default: return true;
        }
    }

    private int m() {
        int n = 0;
        int ii = k0;
        while (true) {
            if (ii > j) return n;
            if (!cons(ii)) break;
            ii++;
        }
        ii++;
        while (true) {
            while (true) {
                if (ii > j) return n;
                if (cons(ii)) break;
                ii++;
            }
            ii++;
            n++;
            while (true) {
                if (ii > j) return n;
                if (!cons(ii)) break;
                ii++;
            }
            ii++;
        }
    }

    private boolean vowelinstem() {
        int ii;
        for (ii = k0; ii <= j; ii++) if (!cons(ii)) return true;
        return false;
    }

    private boolean doublec(int j) {
        if (j < k0 + 1) return false;
        if ((b[j] != b[j - 1])) return false;
        return cons(j);
    }

    private boolean cvc(int i) {
        if (i < k0 + 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false;
        int ch = b[i];
        if (ch == 'w' || ch == 'x' || ch == 'y') return false;
        return true;
    }

    private boolean ends(String s) {
        int l = s.length();
        int o = k - l + 1;
        if (o < k0) return false;
        for (int ii = 0; ii < l; ii++) if (b[o + ii] != s.charAt(ii)) return false;
        j = k - l;
        return true;
    }

    private void setto(String s) {
        int l = s.length();
        int o = j + 1;
        for (int ii = 0; ii < l; ii++) b[o + ii] = s.charAt(ii);
        k = j + l;
        dirty = true;
    }

    private void r(String s) {
        if (m() > 0) setto(s);
    }

    private void step1() {
        if (b[k] == 's') {
            if (ends("sses")) k -= 2;
            else if (ends("ies")) setto("i");
            else if (b[k - 1] != 's') k--;
        }
        if (ends("eed")) { if (m() > 0) k--; } 
        else if ((ends("ed") || ends("ing")) && vowelinstem()) {
            k = j;
            if (ends("at")) setto("ate");
            else if (ends("bl")) setto("ble");
            else if (ends("iz")) setto("ize");
            else if (doublec(k)) {
                k--;
                int ch = b[k];
                if (ch == 'l' || ch == 's' || ch == 'z') k++;
            } else if (m() == 1 && cvc(k)) setto("e");
        }
    }

    private void step2() {
        if (ends("y") && vowelinstem()) { b[k] = 'i'; dirty = true; }
    }

    private void step3() {
        if (k == k0) return;
        switch (b[k - 1]) {
            case 'a':
                if (ends("ational")) { r("ate"); break; }
                if (ends("tional")) { r("tion"); break; }
                break;
            case 'c':
                if (ends("enci")) { r("ence"); break; }
                if (ends("anci")) { r("ance"); break; }
                break;
            case 'e':
                if (ends("izer")) { r("ize"); break; }
                break;
            case 'l':
                if (ends("bli")) { r("ble"); break; }
                if (ends("alli")) { r("al"); break; }
                if (ends("entli")) { r("ent"); break; }
                if (ends("eli")) { r("e"); break; }
                if (ends("ousli")) { r("ous"); break; }
                break;
            case 'o':
                if (ends("ization")) { r("ize"); break; }
                if (ends("ation")) { r("ate"); break; }
                if (ends("ator")) { r("ate"); break; }
                break;
            case 's':
                if (ends("alism")) { r("al"); break; }
                if (ends("iveness")) { r("ive"); break; }
                if (ends("fulness")) { r("ful"); break; }
                if (ends("ousness")) { r("ous"); break; }
                break;
            case 't':
                if (ends("aliti")) { r("al"); break; }
                if (ends("iviti")) { r("ive"); break; }
                if (ends("biliti")) { r("ble"); break; }
                break;
            case 'g':
                if (ends("logi")) { r("log"); break; }
                break;
        }
    }

    private void step4() {
        switch (b[k]) {
            case 'e':
                if (ends("icate")) { r("ic"); break; }
                if (ends("ative")) { r(""); break; }
                if (ends("alize")) { r("al"); break; }
                break;
            case 'i':
                if (ends("iciti")) { r("ic"); break; }
                break;
            case 'l':
                if (ends("ical")) { r("ic"); break; }
                if (ends("ful")) { r(""); break; }
                break;
            case 's':
                if (ends("ness")) { r(""); break; }
                break;
        }
    }

    private void step5() {
        if (k == k0) return;
        switch (b[k - 1]) {
            case 'a':
                if (ends("al")) break; return;
            case 'c':
                if (ends("ance")) break;
                if (ends("ence")) break; return;
            case 'e':
                if (ends("er")) break; return;
            case 'i':
                if (ends("ic")) break; return;
            case 'l':
                if (ends("able")) break;
                if (ends("ible")) break; return;
            case 'n':
                if (ends("ant")) break;
                if (ends("ement")) break;
                if (ends("ment")) break;
                if (ends("ent")) break; return;
            case 'o':
                if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) break;
                if (ends("ou")) break; return;
            case 's':
                if (ends("ism")) break; return;
            case 't':
                if (ends("ate")) break;
                if (ends("iti")) break; return;
            case 'u':
                if (ends("ous")) break; return;
            case 'v':
                if (ends("ive")) break; return;
            case 'z':
                if (ends("ize")) break; return;
            default: return;
        }
        if (m() > 1) k = j;
    }

    private void step6() {
        j = k;
        if (b[k] == 'e') {
            int a = m();
            if (a > 1 || a == 1 && !cvc(k - 1)) k--;
        }
        if (b[k] == 'l' && doublec(k) && m() > 1) k--;
    }

    public String stem(String word) {
        if (word.length() <= 2) return word;
        char[] w = word.toLowerCase().toCharArray();
        b = w;
        i = w.length;
        k0 = 0;
        k = i - 1;
        dirty = false;
        if (k > k0 + 1) {
            step1(); step2(); step3(); step4(); step5(); step6();
        }
        return new String(b, k0, k - k0 + 1);
    }
}
