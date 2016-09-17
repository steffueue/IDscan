package ch.hackzurich.idscan;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * All commented fields can not really be parsed.
 */
public class IdentityCard {
    
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.mm.yyyy");

//    private String type;
//
//    private String code;
//
//    private String passportNr;

    private final String surname;
    
    public String getSurname() {
        return surname;
    }

    private final String givenNames;
    
    public String getGivenNames() {
        return givenNames;
    }

    private final String nationality;
    
    public final String getNationality() {
        return nationality;
    }

    private final Date dateOfBirth;
    
    public final Date getDateOfBirth() {
        return dateOfBirth;
    }
    
    public IdentityCard(String surname, String givenNames, String nationality, Date dateOfBirth) {
        this.surname = surname;
        this.givenNames = givenNames;
        this.nationality = nationality;
        this.dateOfBirth = dateOfBirth;
    }

//    private String placeOfOrigin;
//
//    private String dateOfIssue;
//
//    private String dateOfExpiry;
//
//    private String authority;
//
//    private String height;
//
//    private String sex;
    
    public String toString() {
        return "Surname = " + surname + ", given name: " + givenNames + ", nationality: " + nationality + " dateOfBirth: " + dateOfBirth != null ? simpleDateFormat.format(dateOfBirth) : "";
    }
    
    static class IdentityCardBuilder {
        
        private String surname;
        
        private String givenNames;
        
        private String nationality;
        
        private Date dateOfBirth;
        
        public IdentityCardBuilder setSurname(String surname) {
            this.surname = surname;
            return this;
        }
        
        public IdentityCardBuilder setGivenNames(String givenNames) {
            this.givenNames = givenNames;
            return this;
        }
        
        public IdentityCardBuilder setNationality(String nationality) {
            this.nationality = nationality;
            return this;
        }
        
        public IdentityCardBuilder setDateOfBirth(Date dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }
        
        public IdentityCard build() {
            return new IdentityCard(surname, givenNames, nationality, dateOfBirth);
        }
    
        public Date getDateOfBirth() {
            return dateOfBirth;
        }
    }

}
