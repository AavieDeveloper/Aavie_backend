package com.example.AavieApp.model;

import java.util.List;

/**
 * Aavie — SupplementFormula
 *
 * Computed response object — not a JPA entity.
 * Built by SupplementService from the user's PRAKRITI + PCOS + VIKRITI assessments.
 *
 * Shape consumed by SupplementProduct.tsx:
 * {
 *   prakritiKey:  "PK",
 *   vikritiKey:   "K",
 *   conditions:   ["pcos"],
 *   eyebrow:      "30-day custom formula",
 *   name:         "Aavie Kapha Balance",
 *   vikritiFocus: "Kapha",
 *   tags:         ["Hormonal balance", "Cycle support", "Metabolic support"],
 *   ordered:      false,
 *   herbs:        [ { icon, name, desc, dose, adjusted } ],
 *   condHerbs:    [ { name, dose, desc } ],
 *   dosage: {
 *     am, amNote, pm, pmNote, caution
 *   }
 * }
 */
public class SupplementFormula {

    private String       prakritiKey;    // short key: "V"|"P"|"K"|"VP"|"VK"|"PK"|"T"
    private String       vikritiKey;     // short key: same set
    private List<String> conditions;     // e.g. ["pcos"]

    private String       eyebrow;        // "30-day custom formula"
    private String       name;           // "Aavie Kapha Balance"
    private String       vikritiFocus;   // "Kapha"
    private List<String> tags;
    private boolean      ordered;

    private List<Herb>     herbs;
    private List<CondHerb> condHerbs;
    private Dosage         dosage;

    // ── Inner classes ─────────────────────────────────────────────────────────

    public static class Herb {
        private String  icon;
        private String  name;
        private String  desc;
        private String  dose;
        private boolean adjusted;   // true = reduced dose for this Prakriti

        public Herb(String icon, String name, String desc, String dose, boolean adjusted) {
            this.icon     = icon;
            this.name     = name;
            this.desc     = desc;
            this.dose     = dose;
            this.adjusted = adjusted;
        }

        public String  getIcon()     { return icon; }
        public String  getName()     { return name; }
        public String  getDesc()     { return desc; }
        public String  getDose()     { return dose; }
        public boolean isAdjusted()  { return adjusted; }
    }

    public static class CondHerb {
        private String name;
        private String dose;
        private String desc;

        public CondHerb(String name, String dose, String desc) {
            this.name = name;
            this.dose = dose;
            this.desc = desc;
        }

        public String getName() { return name; }
        public String getDose() { return dose; }
        public String getDesc() { return desc; }
    }

    public static class Dosage {
        private String am;
        private String amNote;
        private String pm;
        private String pmNote;
        private String caution;

        public Dosage(String am, String amNote, String pm, String pmNote, String caution) {
            this.am      = am;
            this.amNote  = amNote;
            this.pm      = pm;
            this.pmNote  = pmNote;
            this.caution = caution;
        }

        public String getAm()      { return am; }
        public String getAmNote()  { return amNote; }
        public String getPm()      { return pm; }
        public String getPmNote()  { return pmNote; }
        public String getCaution() { return caution; }
    }

    // ── Constructors ──────────────────────────────────────────────────────────
    public SupplementFormula() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public String       getPrakritiKey()              { return prakritiKey; }
    public void         setPrakritiKey(String v)      { this.prakritiKey = v; }
    public String       getVikritiKey()               { return vikritiKey; }
    public void         setVikritiKey(String v)       { this.vikritiKey = v; }
    public List<String> getConditions()               { return conditions; }
    public void         setConditions(List<String> v) { this.conditions = v; }
    public String       getEyebrow()                  { return eyebrow; }
    public void         setEyebrow(String v)          { this.eyebrow = v; }
    public String       getName()                     { return name; }
    public void         setName(String v)             { this.name = v; }
    public String       getVikritiFocus()             { return vikritiFocus; }
    public void         setVikritiFocus(String v)     { this.vikritiFocus = v; }
    public List<String> getTags()                     { return tags; }
    public void         setTags(List<String> v)       { this.tags = v; }
    public boolean      isOrdered()                   { return ordered; }
    public void         setOrdered(boolean v)         { this.ordered = v; }
    public List<Herb>   getHerbs()                    { return herbs; }
    public void         setHerbs(List<Herb> v)        { this.herbs = v; }
    public List<CondHerb> getCondHerbs()              { return condHerbs; }
    public void         setCondHerbs(List<CondHerb> v){ this.condHerbs = v; }
    public Dosage       getDosage()                   { return dosage; }
    public void         setDosage(Dosage v)           { this.dosage = v; }
}