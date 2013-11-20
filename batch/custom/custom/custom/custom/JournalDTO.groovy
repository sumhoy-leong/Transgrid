/*
 * Ventyx 2012
 */
package com.mincom.ellipse.script.custom

class JournalDTO {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 1

    private String journalNo;
    private String period;
    private String copyJournal;
    private String standingJournal;
    private String description;
    private String tranDate;
    private String accountant;
    private String accrualJournal;
    private String approvalStatus;
    private ArrayList<JournalEntryDTO> entries
}
