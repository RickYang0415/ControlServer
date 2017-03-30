package Common;

import java.util.ArrayList;

public class Utility {
	public static String[] Split(String regex, String source) {
        ArrayList<String> tempList = new ArrayList<>();
        String[] args = source.split(regex);
        for (String arg : args) {
            if (arg.equals("")) {
                continue;
            }
            tempList.add(arg);
        }
        return tempList.toArray(new String[tempList.size()]);
    }
}
