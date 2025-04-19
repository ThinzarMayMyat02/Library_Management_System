package com.practice.book.email;

public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("activate_account");

    private final String name;
    private EmailTemplateName(String name) {
        this.name = name;
    }
}
