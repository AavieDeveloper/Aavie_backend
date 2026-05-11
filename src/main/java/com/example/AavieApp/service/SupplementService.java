package com.example.AavieApp.service;

import com.example.AavieApp.model.SupplementFormula;
import com.example.AavieApp.model.SupplementFormula.CondHerb;
import com.example.AavieApp.model.SupplementFormula.Dosage;
import com.example.AavieApp.model.SupplementFormula.Herb;
import com.example.AavieApp.model.UserAssessment;
import com.example.AavieApp.repository.UserAssessmentRepository;
import com.example.AavieApp.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Aavie — SupplementService  (v2)
 *
 * Builds a personalised SupplementFormula from the user's three assessments:
 *   PRAKRITI  → prakritiKey  (e.g. "PK")
 *   PCOS      → conditions   (["pcos"] if exists)
 *   VIKRITI   → vikritiKey   (derived from resultType, e.g. "Kapha Vikriti" → "K")
 *
 * Formula logic mirrors SupplementProduct.tsx:
 *   1. Vikriti drives the base herb set and dosage instructions
 *   2. Prakriti filters herb compatibility (care/no = adjusted or excluded)
 *   3. PCOS assessment presence adds condition-specific herbs
 *
 * Herb status values per Prakriti:
 *   "yes"  → included at full dose
 *   "care" → included, flagged as adjusted (reduced dose)
 *   "no"   → excluded
 */
@Service
@Transactional(readOnly = true)
public class SupplementService {

    private static final String TYPE_PRAKRITI = "PRAKRITI";
    private static final String TYPE_PCOS     = "PCOS";
    private static final String TYPE_VIKRITI  = "VIKRITI";

    private final UserAssessmentRepository assessmentRepo;
    private final UserProfileRepository    userRepo;

    public SupplementService(UserAssessmentRepository assessmentRepo,
                             UserProfileRepository userRepo) {
        this.assessmentRepo = assessmentRepo;
        this.userRepo       = userRepo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HERB LIBRARY  —  mirrors H[] array in SupplementProduct.tsx
    // ─────────────────────────────────────────────────────────────────────────

    /** Herb status per DoshaKey. Map key = herb name, value = {doshaKey → status} */
    private static final Map<String, Map<String, String>> HERB_STATUS = new LinkedHashMap<>();

    /**
     * DoshaKey → ordered list of herb names for the base formula.
     * Mirrors MASTER[key].herbs in SupplementProduct.tsx
     */
    private static final Map<String, List<String>> MASTER_HERBS = new LinkedHashMap<>();

    /** DoshaKey → dosage instructions. Mirrors MASTER[key].{am,amNote,pm,pmNote,caution} */
    private static final Map<String, Dosage> MASTER_DOSAGE = new LinkedHashMap<>();

    /** Herb name → {icon, sk (Sanskrit), dose, be (benefit description)} */
    private static final Map<String, String[]> HERB_META = new LinkedHashMap<>();

    /** Condition → list of CondHerb entries */
    private static final Map<String, List<CondHerb>> COND_HERBS = new LinkedHashMap<>();

    /** Fallback herbs used when the vikriti formula produces fewer than 5 compatible herbs */
    private static final List<String> FALLBACK_HERBS =
        List.of("Guduchi","Amalaki","Triphala","Shankhpushpi","Punarnava","Gokshura");

    static {
        // ── Herb status matrix ─────────────────────────────────────────────
        // Format per herb: { "V", "P", "K", "VP", "VK", "PK", "T" } → "yes"|"care"|"no"
        herbStatus("Shatavari",       "yes","yes","care","yes","yes","care","yes");
        herbStatus("Ashwagandha",     "yes","care","care","yes","yes","no", "yes");
        herbStatus("Bala",            "yes","care","no",  "care","care","no","care");
        herbStatus("Dashamoola",      "yes","care","care","yes","yes","care","care");
        herbStatus("Vidari Kanda",    "yes","care","no",  "yes","care","no","care");
        herbStatus("Guduchi",         "yes","yes","yes",  "yes","yes","yes","yes");
        herbStatus("Amalaki",         "yes","yes","yes",  "yes","yes","yes","yes");
        herbStatus("Lodhra",          "care","yes","yes", "yes","care","yes","care");
        herbStatus("Manjistha",       "care","yes","yes", "yes","care","yes","care");
        herbStatus("Neem",            "no", "yes","yes",  "care","no","yes","care");
        herbStatus("Kutki",           "no", "yes","yes",  "care","no","yes","care");
        herbStatus("Pushyanug Churna","care","yes","care","yes","care","care","care");
        herbStatus("Trikatu",         "care","no","yes",  "no","yes","yes","care");
        herbStatus("Kanchanar Guggul","no","care","yes",  "no","care","yes","care");
        herbStatus("Punarnava",       "yes","care","yes", "yes","yes","yes","yes");
        herbStatus("Varuna",          "no","care","yes",  "no","care","yes","care");
        herbStatus("Fenugreek",       "care","care","yes","care","yes","yes","care");
        herbStatus("Triphala",        "yes","yes","yes",  "yes","yes","yes","yes");
        herbStatus("Gokshura",        "yes","yes","care", "yes","yes","care","yes");
        herbStatus("Licorice",        "yes","yes","no",   "yes","care","no","care");
        herbStatus("Shankhpushpi",    "yes","yes","care", "yes","yes","care","yes");
        herbStatus("Turmeric",        "care","care","yes","care","yes","yes","yes");

        // ── Herb metadata: [icon, Sanskrit, dose, benefit] ─────────────────
        HERB_META.put("Shatavari",        new String[]{"🌸","Asparagus racemosus",         "Twice daily","The primary female rasayana. Nourishes reproductive tissue and regulates oestrogen across all phases."});
        HERB_META.put("Ashwagandha",      new String[]{"🌰","Withania somnifera",           "At bedtime","Reduces cortisol and rebuilds ojas. Stabilises the hormonal stress axis — HPA to HPO."});
        HERB_META.put("Bala",             new String[]{"🌿","Sida cordifolia",              "Twice daily","Rebuilds depleted reproductive tissue. Primary herb for Vata-type physical depletion."});
        HERB_META.put("Dashamoola",       new String[]{"🌱","Ten-root classical formula",   "Twice daily","Grounds Apana Vata and relieves uterine spasm. Classical ten-root pelvic formula."});
        HERB_META.put("Vidari Kanda",     new String[]{"🌿","Pueraria tuberosa",            "Twice daily","Deep phytoestrogenic nourishment. Rebuilds reproductive tissue from the root."});
        HERB_META.put("Guduchi",          new String[]{"🍃","Tinospora cordifolia",         "Twice daily","Immunomodulator. Reduces androgen inflammation and improves insulin sensitivity."});
        HERB_META.put("Amalaki",          new String[]{"🫐","Emblica officinalis",          "Twice daily","Liver detox and follicle antioxidant. Cools Pitta and supports oestrogen clearance."});
        HERB_META.put("Lodhra",           new String[]{"🌸","Symplocos racemosa",           "Twice daily","Astringes and tones the uterus. Reduces heavy Pitta bleeding and ovarian congestion."});
        HERB_META.put("Manjistha",        new String[]{"🌺","Rubia cordifolia",             "Twice daily","Deep blood purifier. Clears androgen-driven hormonal excess and skin inflammation."});
        HERB_META.put("Neem",             new String[]{"🍃","Azadirachta indica",           "Twice daily","Anti-androgenic. Reduces testosterone-driven PCOS symptoms."});
        HERB_META.put("Kutki",            new String[]{"🌿","Picrorhiza kurroa",            "Twice daily","Liver tonic. Clears hepatic Pitta and supports oestrogen metabolism."});
        HERB_META.put("Pushyanug Churna", new String[]{"🌼","Classical 28-herb compound",  "Twice daily","Classical formula for heavy or irregular Pitta-type bleeding."});
        HERB_META.put("Trikatu",          new String[]{"🔥","Ginger · Black pepper · Long pepper","Before meals","Kindles Agni and clears Ama from pelvic channels. Metabolic activator."});
        HERB_META.put("Kanchanar Guggul", new String[]{"🌿","Bauhinia variegata compound", "Twice daily","Dissolves ovarian cysts and clears Kapha stagnation from the pelvis."});
        HERB_META.put("Punarnava",        new String[]{"🌱","Boerhavia diffusa",            "Twice daily","Removes water retention and decongests pelvic channels."});
        HERB_META.put("Varuna",           new String[]{"🌿","Crataeva nurvala",             "Twice daily","Dissolves cysts and clears blocked pelvic channels."});
        HERB_META.put("Fenugreek",        new String[]{"🌾","Trigonella foenum-graecum",    "Twice daily","Reduces insulin resistance and androgen levels in metabolic PCOS."});
        HERB_META.put("Triphala",         new String[]{"🫧","Amalaki · Bibhitaki · Haritaki","At bedtime","Year-round tridoshic detox. Supports liver, bowel and oestrogen clearance."});
        HERB_META.put("Gokshura",         new String[]{"🌿","Tribulus terrestris",          "Twice daily","Regulates LH:FSH ratio. Supports ovulation and follicular health."});
        HERB_META.put("Licorice",         new String[]{"🍯","Glycyrrhiza glabra",           "Twice daily","Modulates androgens and cortisol. Harmonises Vata and Pitta simultaneously."});
        HERB_META.put("Shankhpushpi",     new String[]{"🌸","Convolvulus pluricaulis",      "Twice daily","HPO axis nervine. Reduces stress-driven cycle disruption."});
        HERB_META.put("Turmeric",         new String[]{"🌿","Curcuma longa",                "Twice daily","Anti-inflammatory and insulin sensitiser. Broad-spectrum safe across most types."});

        // ── Master herbs per vikriti ───────────────────────────────────────
        MASTER_HERBS.put("V",  List.of("Shatavari","Ashwagandha","Bala","Vidari Kanda","Gokshura","Triphala"));
        MASTER_HERBS.put("P",  List.of("Shatavari","Guduchi","Amalaki","Lodhra","Manjistha","Pushyanug Churna"));
        MASTER_HERBS.put("K",  List.of("Kanchanar Guggul","Trikatu","Guduchi","Punarnava","Fenugreek","Triphala"));
        MASTER_HERBS.put("VP", List.of("Shatavari","Guduchi","Amalaki","Licorice","Shankhpushpi","Gokshura"));
        MASTER_HERBS.put("VK", List.of("Ashwagandha","Shatavari","Punarnava","Trikatu","Fenugreek","Gokshura"));
        MASTER_HERBS.put("PK", List.of("Kanchanar Guggul","Guduchi","Amalaki","Manjistha","Neem","Punarnava"));
        MASTER_HERBS.put("T",  List.of("Shatavari","Guduchi","Amalaki","Triphala","Gokshura","Shankhpushpi"));

        // ── Master dosage per vikriti ──────────────────────────────────────
        MASTER_DOSAGE.put("V",  new Dosage(
            "1 tsp blend in warm full-cream milk with 1 tsp ghee",
            "Before or after breakfast — with ghee for absorption",
            "1 tsp blend in warm full-cream milk with ghee",
            "Most important dose — ojas rebuilding during sleep",
            "Avoid Guggul, Kanchanar Guggul, Trikatu and Neem throughout this protocol."
        ));
        MASTER_DOSAGE.put("P",  new Dosage(
            "1 tsp blend in pomegranate juice or coconut water",
            "Cool or room-temperature carrier only — never warm milk",
            "1 tsp blend in coconut water or plain cool water",
            "30 minutes before sleep — cooling carrier essential",
            "Avoid Trikatu, Cinnamon and all warming herbs. No alcohol, spicy food or fermented foods."
        ));
        MASTER_DOSAGE.put("K",  new Dosage(
            "1 tsp blend in warm water with 1 tsp raw honey — before breakfast",
            "Empty stomach kindles Agni and clears overnight Ama",
            "1 tsp blend in warm water with honey",
            "Never use milk as carrier for this formula",
            "Avoid Shatavari Ghrita, Bala and Vidari Kanda. No dairy, sugar or wheat while on this formula."
        ));
        MASTER_DOSAGE.put("VP", new Dosage(
            "1 tsp blend in warm milk with ghee",
            "Morning carrier: warm milk for Vata",
            "1 tsp blend in pomegranate juice or coconut water",
            "Evening carrier: pomegranate juice for Pitta — rotate daily",
            "Avoid Trikatu, Kanchanar Guggul and high-dose Guggul. No spicy food and no fasting."
        ));
        MASTER_DOSAGE.put("VK", new Dosage(
            "1 tsp blend in warm ginger water with honey",
            "Warm ginger water activates Kapha and grounds Vata",
            "1 tsp blend in warm full-cream milk",
            "Warm milk for bedtime dose only — grounds Vata overnight",
            "Avoid Vidari Kanda and Shatavari Ghrita. No cold food, no excess dairy."
        ));
        MASTER_DOSAGE.put("PK", new Dosage(
            "1 tsp blend in pomegranate juice or aloe vera juice (30ml)",
            "Anti-androgenic and liver-clearing carrier for both doshas",
            "1 tsp blend in Triphala decoction or plain warm water",
            "Never use warm milk as carrier for this formula",
            "Avoid Ashwagandha, Bala and Vidari Kanda. No alcohol, dairy, fried or sweet food."
        ));
        MASTER_DOSAGE.put("T",  new Dosage(
            "1 tsp blend in seasonal carrier",
            "Winter: warm milk. Summer: pomegranate juice. Spring: warm water.",
            "1 tsp blend in seasonal carrier",
            "Rotate carrier with season — consistency is the most important factor",
            "Adjust formula seasonally. Add dosha-specific herbs as dominant dosha shifts each season."
        ));

        // ── Condition add-on herbs ─────────────────────────────────────────
        COND_HERBS.put("pcos", List.of(
            new CondHerb("Kanchanar Guggul", "2 tablets · twice daily",
                "Primary for ovarian cysts. Reduces androgen and normalises LH:FSH ratio. The single most important PCOS herb."),
            new CondHerb("Spearmint (as tea)", "2 cups daily, anytime",
                "Clinically shown to reduce free testosterone. The most evidence-backed PCOS drink.")
        ));
        COND_HERBS.put("thyroid", List.of(
            new CondHerb("Kanchanar Guggul", "2 tablets · twice daily",
                "Thyroid tissue support — under supervision alongside thyroid medications.")
        ));
        COND_HERBS.put("anaemia", List.of(
            new CondHerb("Loha Bhasma + Amalaki", "Morning · with honey",
                "Most bioavailable Ayurvedic iron. Amalaki maximises absorption significantly.")
        ));
        COND_HERBS.put("endo", List.of(
            new CondHerb("Ashoka", "Twice daily",
                "Reduces endometrial overgrowth and heavy bleeding. Classical uterine tonic.")
        ));
        COND_HERBS.put("fibroids", List.of(
            new CondHerb("Varuna", "Twice daily",
                "Reduces fibroid tissue and clears Kapha obstruction in uterine channels.")
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full personalised formula for a user.
     * Called by GET /api/supplement/my-formula  (header: user-id)
     *
     * Requires at least PRAKRITI assessment to be complete.
     * Vikriti falls back to Prakriti key if VIKRITI assessment not yet done.
     */
    public SupplementFormula getFormulaForUser(Long userId) {
        // Verify user exists
        userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found with id: " + userId));

        // ── Fetch assessments ──────────────────────────────────────────────
        Optional<UserAssessment> prakritiOpt =
            assessmentRepo.findByUserIdAndAssessmentType(userId, TYPE_PRAKRITI);
        Optional<UserAssessment> pcosOpt =
            assessmentRepo.findByUserIdAndAssessmentType(userId, TYPE_PCOS);
        Optional<UserAssessment> vikritiOpt =
            assessmentRepo.findByUserIdAndAssessmentType(userId, TYPE_VIKRITI);

        if (prakritiOpt.isEmpty()) {
            throw new RuntimeException("Assessment incomplete. Please complete the Prakriti assessment first.");
        }

        // ── Resolve keys ───────────────────────────────────────────────────
        // prakritiKey is stored on PCOS/VIKRITI as a short key ("PK", "V" etc.)
        // For PRAKRITI itself, derive from resultType
        String prakritiKey = prakritiOpt.get().getPrakritiKey();
        if (prakritiKey == null || prakritiKey.isBlank()) {
            prakritiKey = resultTypeToKey(prakritiOpt.get().getResultType());
        }
        prakritiKey = prakritiKey.trim().toUpperCase();

        // vikriti key derived from VIKRITI resultType (e.g. "Kapha Vikriti" → "K")
        // Falls back to prakritiKey if VIKRITI not done yet
        String vikritiKey;
        if (vikritiOpt.isPresent()) {
            String vikritiResultType = vikritiOpt.get().getResultType();
            // Strip " Vikriti" suffix if present, then map
            String stripped = vikritiResultType.replace(" Vikriti","").trim();
            vikritiKey = resultTypeToKey(stripped);
        } else {
            vikritiKey = prakritiKey;
        }

        // ── Resolve conditions ─────────────────────────────────────────────
        List<String> conditions = new ArrayList<>();
        if (pcosOpt.isPresent()) conditions.add("pcos");
        // Extend here when other conditions (thyroid, anaemia etc.) are added

        // ── Build herb list ────────────────────────────────────────────────
        List<Herb> herbs = buildHerbs(vikritiKey, prakritiKey);

        
     // ── Build condition herbs ──────────────────────────────────────────
        final String pk = prakritiKey;
        List<CondHerb> condHerbs = new ArrayList<>();
        for (String cond : conditions) {
            List<CondHerb> ch = COND_HERBS.getOrDefault(cond, List.of());
            ch.forEach(c -> {
                // Only include if the herb is not 'no' for this Prakriti
                String st = getHerbStatus(c.getName(), pk);
                if (!"no".equals(st)) condHerbs.add(c);
            });
        }

        // ── Dosage ─────────────────────────────────────────────────────────
        Dosage dosage = MASTER_DOSAGE.getOrDefault(vikritiKey, MASTER_DOSAGE.get("T"));

        // ── Formula metadata ───────────────────────────────────────────────
        String formulaName  = buildName(vikritiKey);
        String vikritiFocus = keyToDisplayName(vikritiKey);
        List<String> tags   = buildTags(vikritiKey, conditions);

        // ── Assemble ───────────────────────────────────────────────────────
        SupplementFormula formula = new SupplementFormula();
        formula.setPrakritiKey(prakritiKey);
        formula.setVikritiKey(vikritiKey);
        formula.setConditions(conditions);
        formula.setEyebrow("30-day custom formula");
        formula.setName(formulaName);
        formula.setVikritiFocus(vikritiFocus);
        formula.setTags(tags);
        formula.setOrdered(false);
        formula.setHerbs(herbs);
        formula.setCondHerbs(condHerbs);
        formula.setDosage(dosage);
        return formula;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private List<Herb> buildHerbs(String vikritiKey, String prakritiKey) {
        List<Herb> result = new ArrayList<>();

        List<String> masterList = MASTER_HERBS.getOrDefault(vikritiKey, MASTER_HERBS.get("T"));
        for (String nm : masterList) {
            String st = getHerbStatus(nm, prakritiKey);
            if ("no".equals(st)) continue;
            result.add(makeHerb(nm, "care".equals(st)));
        }

        // Fallback if fewer than 5 herbs passed the filter
        for (String nm : FALLBACK_HERBS) {
            if (result.size() >= 5) break;
            if (result.stream().anyMatch(h -> h.getName().equalsIgnoreCase(nm))) continue;
            String st = getHerbStatus(nm, prakritiKey);
            if ("yes".equals(st)) result.add(makeHerb(nm, false));
        }

        return result;
    }

    private Herb makeHerb(String name, boolean adjusted) {
        String[] meta = HERB_META.get(name);
        if (meta == null) return new Herb("🌿", name, "", "Twice daily", adjusted);
        return new Herb(meta[0], name, meta[3], meta[2], adjusted);
    }

    private String getHerbStatus(String herbName, String doshaKey) {
        Map<String, String> statusMap = HERB_STATUS.get(herbName);
        if (statusMap == null) return "care";
        return statusMap.getOrDefault(doshaKey, "care");
    }

    /**
     * Maps a result string to the short DoshaKey used throughout the app.
     * Handles both display forms ("Pitta-Kapha", "Pitta Kapha") and
     * already-short forms ("PK", "V").
     */
    private String resultTypeToKey(String resultType) {
        if (resultType == null || resultType.isBlank()) return "T";
        String s = resultType.trim().toUpperCase()
            .replace(" ", "-")
            .replace("VIKRITI", "").replace("PCOS", "")
            .replace("--","-").trim().replaceAll("-$","");

        switch (s) {
            case "VATA":         return "V";
            case "PITTA":        return "P";
            case "KAPHA":        return "K";
            case "VATA-PITTA":   return "VP";
            case "PITTA-VATA":   return "VP";
            case "VATA-KAPHA":   return "VK";
            case "KAPHA-VATA":   return "VK";
            case "PITTA-KAPHA":  return "PK";
            case "KAPHA-PITTA":  return "PK";
            case "TRIDOSHIC":    return "T";
            // Already a short key
            case "V": case "P": case "K":
            case "VP": case "VK": case "PK": case "T":
                return s;
            default: return "T";
        }
    }

    private String keyToDisplayName(String key) {
        switch (key) {
            case "V":  return "Vata";
            case "P":  return "Pitta";
            case "K":  return "Kapha";
            case "VP": return "Vata-Pitta";
            case "VK": return "Vata-Kapha";
            case "PK": return "Pitta-Kapha";
            case "T":  return "Tridoshic";
            default:   return key;
        }
    }

    private String buildName(String vikritiKey) {
        return "Aavie " + keyToDisplayName(vikritiKey) + " Balance";
    }

    private List<String> buildTags(String vikritiKey, List<String> conditions) {
        List<String> tags = new ArrayList<>(List.of("Hormonal balance", "Cycle support"));
        switch (vikritiKey) {
            case "P":  tags.add("Anti-inflammatory"); break;
            case "V":  tags.add("Nervous system"); break;
            case "K":  tags.add("Metabolic support"); break;
            case "VP": tags.add("Stress + heat"); break;
            case "PK": tags.add("Inflammation + weight"); break;
            case "VK": tags.add("Energy + grounding"); break;
            default:   tags.add("Tri-doshic"); break;
        }
        if (conditions.contains("pcos")) tags.add("PCOS support");
        return tags;
    }

    // ── Static initializer helper ─────────────────────────────────────────────
    private static void herbStatus(String name,
                                   String V, String P, String K,
                                   String VP, String VK, String PK, String T) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("V", V); m.put("P", P); m.put("K", K);
        m.put("VP", VP); m.put("VK", VK); m.put("PK", PK); m.put("T", T);
        HERB_STATUS.put(name, m);
    }
}