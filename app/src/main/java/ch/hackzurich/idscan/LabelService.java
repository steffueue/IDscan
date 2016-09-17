package ch.hackzurich.idscan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LabelService {
    
    private static final String TAG = LabelService.class.getSimpleName();
    
    private final int highLikelyhood = 70;
    
    private Pattern datePattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
    
    private boolean isLikelyAllCaps(String word) {
        float lowerCaseCharacters = 0;
        float upperCaseCharacters = 0;
        float other = 0;
        float spaces = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c >= 65 && c <= 90) {
                upperCaseCharacters++;
            } else if (c >= 97 && c <= 122) {
                lowerCaseCharacters++;
            } else if (c == ' ') {
                spaces++;
            } else {
                other++;
            }
        }
        
        if ((upperCaseCharacters / (float) (word.length() - spaces)) * 100 >= highLikelyhood) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean isDate(String word) {
        Matcher matcher = datePattern.matcher(word);
        return matcher.matches();
    }
    
    public IdentityCard parse(JSONObject jsonObject) throws JSONException {
        IdentityCard.IdentityCardBuilder identityCardBuilder = new IdentityCard.IdentityCardBuilder();
        final JSONArray regions = jsonObject.getJSONArray("regions");
        int infoIndex = 0;
        for (int i = 0; i < regions.length(); i++) {
            Log.d(TAG, "Region " + (i + 1));
            final JSONArray lines = regions.getJSONObject(i).getJSONArray("lines");
            for (int j = 0; j < lines.length(); j++) {
                Log.d(TAG, "Line " + (j + 1));
                final JSONArray words = lines.getJSONObject(j).getJSONArray("words");
                StringBuilder wordsInLine = new StringBuilder();
                Date date = null;
                for (int k = 0; k < words.length(); k++) {
                    final String word = words.getJSONObject(k).getString("text");
                    wordsInLine.append(word).append(" ");
                    
                    Log.d(TAG, "Word " + (k + 1) + ": " + word);
                }
                boolean use = false;
                String word = wordsInLine.toString().trim();
                if (isLikelyAllCaps(word) && word.length() > 4) {
                    use = true;
                } else if (isDate(word.replaceAll(" ", ""))) {
                    try {
                        date = new SimpleDateFormat("dd.mm.yyyy").parse(word.replaceAll(" ", ""));
                        if (date.getTime() >= System.currentTimeMillis() - (5 * 365 * 24 * 60 * 60 * 1000)) {
                            date = null;
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                if (use && date == null) {
                    switch (infoIndex) {
                        case 0:
                            identityCardBuilder.setSurname(wordsInLine.toString().trim());
                            break;
                        case 1:
                            identityCardBuilder.setGivenNames(wordsInLine.toString().trim());
                            break;
                        case 2:
                            identityCardBuilder.setNationality(wordsInLine.toString().trim());
                            break;
                    }
                    infoIndex++;
                } else if (date != null && identityCardBuilder.getDateOfBirth() == null) { 
                    identityCardBuilder.setDateOfBirth(date);
                }
            }
        }
        return identityCardBuilder.build();
    }
    
}
