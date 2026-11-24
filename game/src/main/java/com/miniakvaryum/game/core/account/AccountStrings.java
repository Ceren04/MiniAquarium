package com.miniakvaryum.game.core.account;

/** User-visible texts of the account screens (sign-up, sign-in, forgot password). */
public final class AccountStrings {

    // Welcome
    static final String WELCOME_SUBTITLE = "Take care of your own little aquarium!";
    static final String SIGN_UP = "Sign up";
    static final String SIGN_IN = "Sign in";

    // Common
    static final String BACK = "Back";
    static final String NEXT = "Next";
    static final String SHOW = "Show";
    static final String HIDE = "Hide";
    static final String STEP = "Step %d of 4";
    static final String REMEMBER_ME_OFF = "[  ] Remember me";
    static final String REMEMBER_ME_ON = "[x] Remember me";
    static final String SAVE_FAILED = "Could not save. Please try again.";

    // About you
    static final String ABOUT_YOU = "About you";
    static final String FIRST_NAME = "First name";
    static final String LAST_NAME = "Last name";
    static final String USERNAME = "Username";
    static final String PASSWORD = "Password";
    static final String REPEAT_PASSWORD = "Repeat password";
    static final String BIRTHDAY = "Birthday";
    static final String DAY = "Day";
    static final String MONTH = "Month";
    static final String YEAR = "Year";
    static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August",
            "September", "October", "November", "December"};

    static final String ERROR_FIRST_NAME = "Please enter your first name";
    static final String ERROR_LAST_NAME = "Please enter your last name";
    static final String ERROR_USERNAME_FORMAT = "3 to 15 letters, digits or _";
    static final String ERROR_USERNAME_TAKEN = "This username is taken";
    static final String ERROR_PASSWORD_SHORT = "At least 6 characters";
    static final String ERROR_PASSWORD_LONG = "At most 64 characters";
    static final String ERROR_PASSWORDS_DIFFER = "Passwords do not match";
    static final String ERROR_BIRTHDAY = "Please check your birthday";

    // Safety color
    static final String SAFETY_TITLE = "Your safety color";
    static final String SAFETY_TEXT = "Pick your favorite color.\nIf you ever forget your password, we will ask you for this color.";
    static final String[] COLOR_NAMES = {"Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple", "Pink",
            "Brown", "Gray"};

    // Picture
    static final String PICTURE_TITLE = "Your picture";
    static final String PICTURE_TEXT = "Pick a picture, or skip and use your first letter.";
    static final String SKIP = "Skip";

    // All set
    static final String ALL_SET = "All set!";
    static final String WELCOME_NAME = "Welcome, %s!";
    static final String START = "Start";

    // Sign in
    static final String SIGN_IN_ACCOUNTS = "Tap your picture:";
    static final String NO_ACCOUNTS = "There is no account on this device yet.";
    static final String WRONG_SIGN_IN = "Wrong username or password";
    static final String LOCKED_SECONDS = "Too many tries. Please wait %d seconds.";
    static final String LOCKED_MINUTES = "Too many tries. Please wait %d minutes.";
    static final String FORGOT_PASSWORD = "Forgot password?";

    // Forgot password
    static final String WHO_ARE_YOU = "Who are you?";
    static final String WHAT_COLOR = "What is your favorite color?";
    static final String WRONG_COLOR = "That's not your color. Try again.";
    static final String NEW_PASSWORD_TITLE = "Choose a new password";
    static final String NEW_PASSWORD = "New password";
    static final String SAVE = "Save";

    /** Button on the profile screen. */
    public static final String SIGN_OUT = "Sign out";
    static final String SIGN_OUT_QUESTION = "Do you really want to sign out?";
    static final String YES_SIGN_OUT = "Yes, sign out";
    static final String NO = "No";

    // Edit profile
    static final String EDIT_PROFILE = "Edit profile";
    static final String CHANGE_PICTURE = "Change picture";
    static final String SAVE_NAMES = "Save names";
    static final String NAMES_SAVED = "Names saved";
    static final String CURRENT_PASSWORD = "Current password";
    static final String CHANGE_PASSWORD = "Change password";
    static final String PASSWORD_CHANGED = "Password changed";
    static final String WRONG_PASSWORD = "Wrong password";
    static final String CHANGE_SAFETY_COLOR = "Change safety color";
    static final String PICK_NEW_COLOR = "Type your password and pick your new color.";
    static final String NOT_CHANGEABLE = "Username @%s and birthday %s cannot be changed.";
    static final String DONE = "Done";

    // Picture chooser
    static final String CHOOSE_FROM_GALLERY = "Choose from gallery";
    static final String USE_FIRST_LETTER = "Use first letter";
    static final String PHOTO_FAILED = "This picture could not be opened.";

    // Adjust photo
    static final String ADJUST_TITLE = "Adjust your photo";
    static final String ADJUST_TEXT = "Drag the photo, zoom with + and - (or the mouse wheel).";
    static final String USE_PHOTO = "Use photo";

    private AccountStrings() {
    }
}
