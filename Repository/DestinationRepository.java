package repository;

import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for destinations (used by HomeController,
 * RecommendationController and any future admin screen).
 *
 * Filtering rule: REGION -> CATEGORY -> DESTINATION. Every destination keeps
 * its own activity list, so activities are never mixed between destinations.
 */
public class DestinationRepository {

    public static final String LUZON = "Luzon";
    public static final String VISAYAS = "Visayas";
    public static final String MINDANAO = "Mindanao";

    public static final String BEACH = "Beach";
    public static final String MOUNTAIN = "Mountain";
    public static final String CITY = "City";
    public static final String ADVENTURE = "Adventure";
    public static final String CULTURAL = "Cultural";

    public static final List<String> REGIONS = List.of(LUZON, VISAYAS, MINDANAO);
    public static final List<String> CATEGORIES =
            List.of(BEACH, MOUNTAIN, CITY, ADVENTURE, CULTURAL);

    private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_DESCRIPTIONS = new LinkedHashMap<>();
    private static final Map<String, String> REGION_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        CATEGORY_LABELS.put(BEACH, "Beach & Island");
        CATEGORY_LABELS.put(MOUNTAIN, "Mountain");
        CATEGORY_LABELS.put(CITY, "City & Urban");
        CATEGORY_LABELS.put(ADVENTURE, "Adventure");
        CATEGORY_LABELS.put(CULTURAL, "Cultural & Historical");

        CATEGORY_DESCRIPTIONS.put(BEACH, "White sand, clear water, reefs and island hopping.");
        CATEGORY_DESCRIPTIONS.put(MOUNTAIN, "Summits, volcanoes and scenic highland views.");
        CATEGORY_DESCRIPTIONS.put(CITY, "Food, shopping, nightlife and modern city life.");
        CATEGORY_DESCRIPTIONS.put(ADVENTURE, "Waterfalls, trails, surf and outdoor thrills.");
        CATEGORY_DESCRIPTIONS.put(CULTURAL, "Heritage towns, museums, churches and traditions.");

        REGION_DESCRIPTIONS.put(LUZON, "Discover mountains, beaches, cities, adventure, and rich history.");
        REGION_DESCRIPTIONS.put(VISAYAS, "Explore beautiful islands, beaches, culture, and adventure.");
        REGION_DESCRIPTIONS.put(MINDANAO, "Experience spectacular mountains, waterfalls, islands, and culture.");
    }

    // ---------- singleton ----------
    private static final DestinationRepository INSTANCE = new DestinationRepository();

    public static DestinationRepository getInstance() {
        return INSTANCE;
    }

    private final List<Destination> destinations = new ArrayList<>();

    private DestinationRepository() {
        seedLuzon();
        seedVisayas();
        seedMindanao();
    }

    // =====================================================
    // QUERIES
    // =====================================================

    public List<Destination> getAll() {
        return Collections.unmodifiableList(destinations);
    }

    public List<Destination> getByRegion(String region) {
        List<Destination> result = new ArrayList<>();
        for (Destination d : destinations) {
            if (d.getRegion().equalsIgnoreCase(region)) {
                result.add(d);
            }
        }
        return result;
    }

    /** Destinations in ONE region AND ONE category (the main browsing filter). */
    public List<Destination> getByRegionAndCategory(String region, String category) {
        List<Destination> result = new ArrayList<>();
        for (Destination d : getByRegion(region)) {
            if (d.hasCategory(category)) {
                result.add(d);
            }
        }
        return result;
    }

    public List<Destination> getByCategory(String category) {
        List<Destination> result = new ArrayList<>();
        for (Destination d : destinations) {
            if (d.hasCategory(category)) {
                result.add(d);
            }
        }
        return result;
    }

    /** Search by name, province, region or category (case-insensitive). */
    public List<Destination> search(String query) {
        String q = query.trim().toLowerCase();
        List<Destination> result = new ArrayList<>();
        for (Destination d : destinations) {
            boolean match = d.getName().toLowerCase().contains(q)
                    || d.getProvince().toLowerCase().contains(q)
                    || d.getRegion().equalsIgnoreCase(q)
                    || d.getCategories().stream().anyMatch(c ->
                            c.equalsIgnoreCase(q) || label(c).equalsIgnoreCase(q));
            if (match) {
                result.add(d);
            }
        }
        return result;
    }

    public Destination findByName(String name) {
        for (Destination d : destinations) {
            if (d.getName().equalsIgnoreCase(name)) {
                return d;
            }
        }
        return null;
    }

    /** Number of destinations per category inside one region. */
    public Map<String, Integer> countByCategory(String region) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String category : CATEGORIES) {
            counts.put(category, getByRegionAndCategory(region, category).size());
        }
        return counts;
    }

    // =====================================================
    // ADMIN (add / remove)
    // =====================================================

    /** @return false if a destination with the same name already exists. */
    public boolean add(Destination destination) {
        if (findByName(destination.getName()) != null) {
            return false;
        }
        destinations.add(destination);
        return true;
    }

    public boolean remove(String name) {
        Destination d = findByName(name);
        return d != null && destinations.remove(d);
    }

    // =====================================================
    // LABELS & CATEGORY-LEVEL INFO
    // =====================================================

    public static String label(String category) {
        return CATEGORY_LABELS.getOrDefault(category, category);
    }

    public static String categoryDescription(String category) {
        return CATEGORY_DESCRIPTIONS.getOrDefault(category, "");
    }

    public static String regionDescription(String region) {
        return REGION_DESCRIPTIONS.getOrDefault(region, "");
    }

    public static List<String> whatToBring(String category) {
        switch (category) {
            case BEACH:
                return List.of("Swimwear", "Reef-safe sunscreen", "Dry bag", "Water shoes", "Snorkel gear (optional)");
            case MOUNTAIN:
                return List.of("Trekking shoes", "Warm jacket", "Headlamp", "Rain gear", "Water and trail snacks");
            case CITY:
                return List.of("Comfortable shoes", "Power bank", "Umbrella", "Light bag", "Cash and cards");
            case ADVENTURE:
                return List.of("Quick-dry clothes", "Sturdy sandals or shoes", "Dry bag", "Sunscreen", "Water");
            case CULTURAL:
                return List.of("Comfortable walking shoes", "Hat and sunscreen", "Camera", "Modest clothing for churches", "Cash for entrance fees");
            default:
                return List.of("Water", "Sunscreen", "Valid ID");
        }
    }

    public static List<String> travelTips(String category) {
        switch (category) {
            case BEACH:
                return List.of("Book boats and island tours ahead in peak season.", "Check sea and weather conditions before going.", "Do not touch or step on corals.");
            case MOUNTAIN:
                return List.of("Hire a registered local guide.", "Start early and check weather and park advisories.", "Pack out all your trash.");
            case CITY:
                return List.of("Travel off-peak to avoid heavy traffic.", "Use ride-hailing or trains for longer distances.", "Keep valuables secure in crowded areas.");
            case ADVENTURE:
                return List.of("Follow your guide's safety briefing.", "Wear a life vest or helmet where provided.", "Avoid risky activities after heavy rain.");
            case CULTURAL:
                return List.of("Go early to avoid crowds and heat.", "Hire a local guide for historical context.", "Respect rules at churches and heritage sites.");
            default:
                return List.of("Plan ahead and travel responsibly.");
        }
    }

    // =====================================================
    // SEED DATA
    // add(region, name, province, categories, description, score,
    //     bestTime, duration, difficulty, budget, activities, places)
    // activities separated by ";"  |  places as "Name|description" separated by ";"
    // =====================================================

    private void seedLuzon() {
        add(LUZON, "La Mesa Watershed Reserve", "Quezon City", "Adventure",
                "A protected forest reserve inside Metro Manila with trails, a reservoir and rich birdlife.",
                4.4, "Nov–Apr (dry season)", "Half day", "Easy", "₱300–1,000 per person",
                "Hiking;Mountain biking;Nature walking;Bird watching;Wildlife observation",
                "La Mesa Ecopark|Trails, picnic areas and a nature center;Reservoir viewpoint|Calm water and watershed views;Forest trails|Shaded paths for walking and biking");

        add(LUZON, "Mount Pinatubo & Crater Lake", "Zambales / Tarlac / Pampanga", "Adventure",
                "Cross lahar canyons by 4x4, then trek to a turquoise crater lake inside a volcano.",
                4.8, "Nov–May", "1 day (7–9 hours)", "Moderate", "₱2,500–4,000 per person",
                "4x4 off-road adventure;Volcano trekking;Crater lake viewing;Hiking;Landscape photography",
                "4x4 river-bed ride|Rough ride to the trailhead;Lahar canyon trail|Moon-like canyon walls along the trek;Crater Lake|Turquoise lake inside the caldera");

        add(LUZON, "Baler", "Aurora", "Adventure",
                "A laid-back coastal town known as the birthplace of Philippine surfing.",
                4.6, "Oct–Mar (surf season)", "2–3 days", "Easy to moderate", "₱5,000–9,000 per person",
                "Surfing;Beach exploration;Coastal hiking;Island hopping;Sunrise photography",
                "Sabang Beach|Main surf beach and town hub;Diguisit rock formations|Rugged coastal rocks and beach;Ditumabo Falls|Waterfall reached by a short trek");

        add(LUZON, "Intramuros & Fort Santiago", "Manila", "Cultural",
                "The Spanish-era walled city with centuries-old churches, plazas and the historic Fort Santiago.",
                4.7, "Nov–Feb", "Half day to 1 day", "Easy", "₱500–2,000 per person",
                "Heritage walking tour;Fort Santiago tour;Historical photography;Church visits;Museum visits",
                "Fort Santiago|Citadel tied to José Rizal's imprisonment;San Agustin Church|UNESCO-listed baroque church;Casa Manila|Colonial-era house museum");

        add(LUZON, "Binondo", "Manila", "Cultural",
                "The world's oldest Chinatown, known for its food, shops and heritage churches.",
                4.6, "Year-round", "Half day", "Easy", "₱500–1,500 per person",
                "Chinatown food tour;Heritage walking tour;Food photography;Shopping;Cultural exploration",
                "Binondo Church|Historic church at the heart of the district;Ongpin Street|Food stalls, bakeries and shops;Chinese-Filipino eateries|Classic dumplings, noodles and hopia");

        add(LUZON, "National Museum Complex", "Manila", "Cultural",
                "A cluster of museums covering fine arts, anthropology and natural history.",
                4.6, "Year-round", "Half day", "Easy", "₱200–800 per person",
                "Art viewing;Natural history exploration;Anthropology exhibits;Educational tour;Photography",
                "National Museum of Fine Arts|Home to the Spoliarium;National Museum of Natural History|Tree of Life atrium and exhibits;National Museum of Anthropology|Archaeology and ethnography");

        add(LUZON, "Calle Crisologo / Vigan City", "Ilocos Sur", "Cultural",
                "A UNESCO World Heritage town with cobblestone streets and Spanish colonial houses.",
                4.8, "Nov–Feb", "2 days", "Easy", "₱3,000–6,000 per person",
                "Heritage walking tour;Kalesa ride;Colonial architecture photography;Local food tasting;Souvenir shopping",
                "Calle Crisologo|Cobbled heritage street;Plaza Salcedo & Vigan Cathedral|Central plaza and cathedral;Pagburnayan Pottery|Traditional jar-making workshops");

        add(LUZON, "Batad Rice Terraces", "Ifugao", "Cultural",
                "Amphitheater-shaped terraces carved by the Ifugao people, part of a UNESCO site.",
                4.8, "Mar–Jun (green terraces)", "2–3 days", "Moderate", "₱3,500–7,000 per person",
                "Rice terrace hiking;Village exploration;Cultural immersion;Landscape photography;Sunrise viewing",
                "Batad viewpoint|Panoramic view of the terraces;Tappiya Falls|Waterfall below the village;Batad village|Traditional Ifugao homes and paths");

        add(LUZON, "Sagada", "Mountain Province", "Cultural",
                "A cool mountain town known for hanging coffins, caves and Cordilleran culture.",
                4.7, "Nov–Feb", "3 days", "Moderate", "₱4,000–8,000 per person",
                "Hanging Coffins tour;Cave exploration;Cultural tour;Mountain hiking;Sunrise viewing",
                "Echo Valley|Hanging coffins viewpoint;Sumaguing Cave|Large cave with rock formations;Kiltepan Viewpoint|Sunrise over the clouds");

        add(LUZON, "Bonifacio Global City (BGC)", "Taguig", "City",
                "A modern district of high-rises, restaurants, art and nightlife.",
                4.5, "Year-round", "Half day to 1 day", "Easy", "₱1,500–4,000 per person",
                "Food trips;Shopping;Street art tour;Nightlife;Photography",
                "High Street|Open-air shopping and dining;The Mind Museum|Interactive science museum;BGC street art|Murals around the district");

        add(LUZON, "Poblacion", "Makati City", "City",
                "Makati's lively dining and nightlife district of bars, cafés and murals.",
                4.4, "Year-round", "Evening to half day", "Easy", "₱1,500–4,000 per person",
                "Restaurant hopping;Café hopping;Nightlife;Street art exploration;Local food tasting",
                "Kalayaan Avenue strip|Bars and restaurants;Poblacion murals|Wall art across the district;Neighborhood cafés|Specialty coffee spots");

        add(LUZON, "Ayala Triangle & Greenbelt", "Makati City", "City",
                "A green urban park beside Greenbelt's shops, restaurants and chapel.",
                4.5, "Year-round", "Half day", "Easy", "₱1,500–4,000 per person",
                "Shopping;Restaurant dining;Park walking;Café hopping;Night photography",
                "Ayala Triangle Gardens|Open green space in the business district;Greenbelt Park|Landscaped gardens and chapel;Ayala Museum|Philippine history and art");

        add(LUZON, "San Fernando City", "La Union", "City",
                "The capital of La Union and a gateway to surf beaches, food spots and coastal towns.",
                4.4, "Nov–Apr", "2–3 days", "Easy", "₱3,500–7,000 per person",
                "Food exploration;City sightseeing;Cultural exploration;Shopping;Coastal activities",
                "Poro Point|Coastal area with a lighthouse;Ma-Cho Temple|Taoist temple;Urbiztondo Beach (nearby)|Popular surf beach");

        add(LUZON, "Mount Pulag", "Benguet / Ifugao / Nueva Vizcaya", "Mountain",
                "Famous for its sea of clouds and sweeping mountain views.",
                4.9, "Nov–Feb (sea of clouds)", "2 days, 1 night", "Moderate", "₱3,000–6,000 per person",
                "Mountain trekking;Sea of clouds viewing;Sunrise photography;Camping;Nature photography",
                "Ambangeg Trail|The most popular route to the summit;Camp 2|Camping area near the summit;Summit grassland|Sunrise and sea of clouds");

        add(LUZON, "Mayon Volcano", "Albay", "Mountain",
                "A near-perfect cone volcano and the icon of Bicol.",
                4.8, "Feb–May (clearer skies)", "1–2 days", "Easy to moderate (check PHIVOLCS alerts)", "₱2,500–5,000 per person",
                "ATV riding;Volcano sightseeing;Hiking;Photography;Nature exploration",
                "Cagsawa Ruins|Church ruins with Mayon as the backdrop;Lignon Hill|Viewpoint over Legazpi and Mayon;Lava trail ATV route|Off-road trail on old lava flows");

        add(LUZON, "Batanes", "Batanes", "Beach",
                "Rolling hills, windswept cliffs and stone houses on the northern frontier islands.",
                4.9, "Mar–Jun", "4 days, 3 nights", "Easy", "₱18,000–30,000 per person",
                "Island sightseeing;Coastal hiking;Cycling;Photography;Cultural village visits",
                "Basco Lighthouse|Hilltop lighthouse with ocean views;Sabtang Island|Stone-house villages;Valugan Boulder Beach|Dramatic boulder-strewn shore");

        add(LUZON, "Hundred Islands", "Pangasinan", "Beach",
                "A cluster of limestone islets in Lingayen Gulf, ideal for island hopping.",
                4.5, "Nov–May", "1–2 days", "Easy", "₱2,000–4,500 per person",
                "Island hopping;Snorkeling;Kayaking;Swimming;Cliff jumping",
                "Governor's Island|Viewdeck and cave;Quezon Island|Beach and picnic area;Children's Island|Shallow, calm swimming");

        add(LUZON, "Coron", "Palawan", "Beach",
                "Limestone cliffs, clear lagoons and famous WWII shipwreck dives.",
                4.9, "Nov–May", "3 days, 2 nights", "Easy to moderate", "₱8,000–15,000 per person",
                "Island hopping;Lagoon swimming;Snorkeling;Scuba diving;Shipwreck diving",
                "Kayangan Lake|Crystal-clear lake among cliffs;Twin Lagoon|Two lagoons linked by a narrow gap;Siete Pecados Marine Park|Snorkeling over healthy reef");

        add(LUZON, "El Nido", "Palawan", "Beach",
                "Dramatic karst cliffs, hidden lagoons and white-sand beaches.",
                4.9, "Nov–May", "3–4 days", "Easy to moderate", "₱9,000–18,000 per person",
                "Island hopping;Lagoon exploration;Kayaking;Snorkeling;Beach swimming",
                "Big Lagoon|Emerald lagoon framed by cliffs;Small Lagoon|Kayak through a narrow entrance;Nacpan Beach|Long, quiet white-sand beach");

        add(LUZON, "Apo Reef Natural Park", "Occidental Mindoro", "Beach",
                "A protected marine park and one of the largest coral reef systems in the Philippines.",
                4.8, "Mar–Jun", "2–3 days", "Moderate (dive certification for diving)", "₱8,000–16,000 per person",
                "Scuba diving;Snorkeling;Marine wildlife viewing;Island hopping;Underwater photography",
                "Reef drop-offs|Dive walls with sharks and turtles;Shallow reef areas|Easy snorkeling;Sandbar islets|Small islets inside the park");

        add(LUZON, "Las Piñas–Parañaque Critical Habitat", "Las Piñas / Parañaque", "Beach",
                "A mangrove and lagoon wetland on Manila Bay, a refuge for migratory birds.",
                4.2, "Nov–Mar (migratory birds)", "Half day", "Easy", "₱300–1,000 per person",
                "Bird watching;Nature photography;Wetland exploration;Environmental education;Wildlife observation",
                "Mangrove forest|Paddle or boat through the mangroves;Lagoon and islets|Roosting grounds for birds;Bird-watching areas|Best at dawn and dusk");

        add(LUZON, "Ninoy Aquino Parks & Wildlife Center", "Quezon City", "Beach",
                "A city wildlife park with gardens, rescued animals and nature trails.",
                4.2, "Nov–Apr", "Half day", "Easy", "₱200–800 per person",
                "Nature walking;Wildlife observation;Bird watching;Educational tours;Photography",
                "Garden trails|Shaded paths for walking;Wildlife areas|Native animals and birds;Picnic grounds|Family-friendly green space");
    }

    private void seedVisayas() {
        add(VISAYAS, "Kawasan Falls", "Badian, Cebu", "Adventure",
                "Turquoise waterfalls and the starting point for Cebu's famous canyoneering.",
                4.8, "Nov–May", "1 day", "Moderate", "₱2,000–4,500 per person",
                "Canyoneering;Cliff jumping;Swimming;Waterfall exploration;Hiking",
                "Kawasan Falls basin|Bright blue pools;Matutinao River canyon|Canyoneering route;Upper tiers|Quieter cascades");

        add(VISAYAS, "Biri Island", "Northern Samar", "Adventure",
                "Dramatic rock formations carved by waves along the Samar coast.",
                4.5, "Mar–Jun", "2 days, 1 night", "Moderate", "₱3,500–7,000 per person",
                "Rock formation sightseeing;Coastal hiking;Photography;Tide pool exploration;Island exploration",
                "Magasang Rock Formation|Towering sea-sculpted rocks;Bel-at Rock Formation|Layered coastal cliffs;Tide pools|Pools exposed at low tide");

        add(VISAYAS, "Cebu City Heritage Sites", "Cebu", "Cultural",
                "The historic core of the oldest Spanish city in the Philippines.",
                4.6, "Nov–Apr", "1 day", "Easy", "₱800–2,000 per person",
                "Heritage walking tour;Magellan's Cross visit;Basilica del Santo Niño visit;Fort San Pedro tour;Historical photography",
                "Magellan's Cross|Symbol of Spanish arrival in 1521;Basilica Minore del Santo Niño|Oldest Roman Catholic church in the country;Fort San Pedro|Spanish defensive fort");

        add(VISAYAS, "The Ruins", "Talisay, Negros Occidental", "Cultural",
                "The remains of a grand sugar baron's mansion, often called the Taj Mahal of Negros.",
                4.6, "Nov–Apr", "Half day", "Easy", "₱1,500–3,500 per person",
                "Heritage tour;Architecture photography;Garden walking;Sunset viewing;Historical exploration",
                "Mansion ruins|Italianate columns and walls;Gardens|Landscaped grounds;Sunset viewing area|Golden light on the ruins");

        add(VISAYAS, "Cebu City", "Cebu", "City",
                "The Philippines' oldest city, mixing heritage, food and a busy modern center.",
                4.5, "Nov–Apr", "2 days", "Easy", "₱3,000–6,000 per person",
                "City sightseeing;Food trips;Shopping;Heritage tours;Nightlife",
                "Colon Street|Oldest street in the country;Lechon eateries|Cebu's famous roast pork;Mall and nightlife districts|Shopping and evening entertainment");

        add(VISAYAS, "Chocolate Hills", "Bohol", "Mountain",
                "Over a thousand cone-shaped hills that turn brown in the dry season.",
                4.7, "Dec–May", "Half day", "Easy", "₱2,000–4,500 per person",
                "Viewpoint sightseeing;Hiking;Photography;Cycling;Countryside exploration",
                "Chocolate Hills Complex|Main viewing deck in Carmen;Sagbayan Peak|Quieter viewpoint;Bilar Man-made Forest|Tree-lined road");

        add(VISAYAS, "White Beach & Willy's Rock", "Boracay, Aklan", "Beach",
                "Boracay's famous powder-white beach and its iconic rock shrine.",
                4.8, "Nov–May", "3–4 days", "Easy", "₱8,000–16,000 per person",
                "Swimming;Beach walking;Sunset viewing;Island hopping;Water sports",
                "White Beach Station 1|Quiet end of the beach;Willy's Rock|Rock formation with a small shrine;Puka Shell Beach|Quieter beach on the north");

        add(VISAYAS, "Panglao Island", "Bohol", "Beach",
                "Beaches, coral reefs and dive sites just off Bohol.",
                4.8, "Nov–May", "3 days, 2 nights", "Easy", "₱6,000–12,000 per person",
                "Snorkeling;Scuba diving;Swimming;Island hopping;Beach relaxation",
                "Alona Beach|Main beach with restaurants and dive shops;Balicasag Island|Marine sanctuary with turtles;Virgin Island|Sandbar visited on island-hopping tours");

        add(VISAYAS, "Moalboal", "Cebu", "Beach",
                "Home of the famous sardine run and easy shore diving.",
                4.8, "Dec–May", "2 days, 1 night", "Easy", "₱3,500–7,000 per person",
                "Sardine run snorkeling;Scuba diving;Sea turtle watching;Swimming;Island hopping",
                "Panagsama Beach|Dive and snorkel hub;Sardine run|Huge schools right off the shore;Pescador Island|Reef and wall diving");

        add(VISAYAS, "Gigantes Islands", "Carles, Iloilo", "Beach",
                "Quiet islands with sandbars, white beaches and fresh seafood.",
                4.6, "Mar–Jun", "2 days, 1 night", "Easy", "₱3,500–7,000 per person",
                "Island hopping;Snorkeling;Sandbar exploration;Seafood tasting;Beach swimming",
                "Antonia Beach|Powdery white beach;Cabugao Gamay Island|Small island with white sand;Bantigue Sandbar|Sandbar visible at low tide");

        add(VISAYAS, "Siquijor Island & Cambugahay Falls", "Siquijor", "Beach",
                "A mystical island of quiet beaches and a multi-tiered turquoise waterfall.",
                4.7, "Nov–May", "3 days, 2 nights", "Easy", "₱5,000–9,000 per person",
                "Waterfall swimming;Island exploration;Beach hopping;Cliff jumping;Nature photography",
                "Cambugahay Falls|Tiered falls with rope swings;Salagdoong Beach|Beach with cliff-jumping platforms;Century-old balete tree|Giant tree in Lazi");

        add(VISAYAS, "Apo Island", "Dauin, Negros Oriental", "Beach",
                "A marine sanctuary famous for sea turtles and healthy coral.",
                4.8, "Mar–Jun", "1 day", "Easy", "₱2,500–5,500 per person",
                "Sea turtle snorkeling;Scuba diving;Coral reef exploration;Swimming;Underwater photography",
                "Marine sanctuary|Protected reef;Sea turtle snorkeling area|Turtles in shallow water;Apo Island beach|Small village beach");

        add(VISAYAS, "Philippine Tarsier Sanctuary", "Corella, Bohol", "Beach",
                "A forest sanctuary where visitors can see the tiny Philippine tarsier.",
                4.4, "Year-round", "1–2 hours", "Easy", "₱1,000–2,500 per person",
                "Tarsier observation;Nature walking;Wildlife photography;Educational tour;Conservation learning",
                "Tarsier viewing trail|Quiet forest path;Visitor information area|Learn about conservation;Forest walk|Short nature trail");
    }

    private void seedMindanao() {
        add(MINDANAO, "Dahilayan Adventure Park", "Manolo Fortich, Bukidnon", "Adventure",
                "A highland adventure park with ziplines, rides and cool mountain air.",
                4.6, "Nov–May", "1 day", "Moderate", "₱2,500–5,000 per person",
                "Ziplining;Mountain coaster;Forest trekking;Adventure rides;Nature photography",
                "Zipline course|Flight over the forest;Mountain coaster|Ride down the hillside;Forest trails|Walks among pine trees");

        add(MINDANAO, "Tinuy-an Falls", "Bislig, Surigao del Sur", "Adventure",
                "A wide, multi-tiered curtain waterfall often called the Niagara of the Philippines.",
                4.7, "Mar–Jun", "1–2 days", "Easy", "₱3,000–6,000 per person",
                "Waterfall swimming;Trekking;Photography;Nature exploration;River activities",
                "Main falls|Wide curtain of water;Bamboo raft ride|Ride close to the falls;Upper tiers|Quieter cascades");

        add(MINDANAO, "Enchanted River", "Hinatuan, Surigao del Sur", "Adventure",
                "A deep blue spring that flows into the sea, known for its clear water.",
                4.8, "Year-round", "1 day", "Easy", "₱3,000–6,000 per person",
                "Swimming;River sightseeing;Snorkeling;Photography;Nature exploration",
                "Deep blue spring|The river's main pool;Viewing platform|Photo spot over the water;Riverside trail|Short walk by the river");

        Destination davao = add(MINDANAO, "Davao City", "Davao del Sur", "City,Cultural",
                "Mindanao's biggest city, mixing markets, food and local culture.",
                4.6, "Year-round", "3 days", "Easy", "₱4,000–8,000 per person",
                "Cultural tours;Local market exploration;Food trips;Heritage sightseeing;Local crafts shopping;City sightseeing;Food exploration;Shopping;Night market visits;Cultural experiences",
                "People's Park|Central city park with sculptures;Roxas Night Market|Street food and evening shopping;Philippine Eagle Center|Conservation center for the national bird;Aldevinco Shopping Center|Local crafts and souvenirs");
        // Davao has different activities depending on the category the user picked
        davao.setActivities(CITY, Arrays.asList(
                "City sightseeing", "Food exploration", "Shopping", "Night market visits", "Cultural experiences"));
        davao.setActivities(CULTURAL, Arrays.asList(
                "Cultural tours", "Local market exploration", "Food trips", "Heritage sightseeing", "Local crafts shopping"));

        add(MINDANAO, "Mount Apo", "Davao del Sur / Cotabato", "Mountain",
                "The highest mountain in the Philippines at 2,954 meters.",
                4.8, "Mar–May", "3 days, 2 nights", "Hard", "₱6,000–10,000 per person",
                "Mountain trekking;Camping;Wildlife observation;Sunrise viewing;Nature photography",
                "Lake Venado|Alpine lake along the trail;Solfatara fields|Steaming volcanic vents;Summit|The country's highest point");

        add(MINDANAO, "Mount Dulang-Dulang", "Bukidnon", "Mountain",
                "The second-highest peak in the Philippines, in the Kitanglad Range.",
                4.5, "Mar–May", "3 days, 2 nights", "Hard", "₱5,000–9,000 per person",
                "Mountain trekking;Forest exploration;Camping;Nature photography;Wildlife observation",
                "Kitanglad Range Natural Park|Protected highland forest;Mossy forest|Cool, misty forest trail;Summit|Views across Bukidnon");

        add(MINDANAO, "Siargao Island", "Surigao del Norte", "Beach",
                "The surfing capital of the Philippines, with lagoons and island-hopping spots.",
                4.9, "Aug–Nov (surf), Mar–Jun (calmer seas)", "4 days, 3 nights", "Easy", "₱9,000–18,000 per person",
                "Surfing;Island hopping;Lagoon exploration;Swimming;Sunset viewing",
                "Cloud 9|Famous surf break and boardwalk;Sugba Lagoon|Clear blue lagoon;Naked, Daku and Guyam Islands|Classic island-hopping trio");

        add(MINDANAO, "Camiguin Island", "Camiguin", "Beach",
                "A small volcanic island with hot springs, waterfalls and white sandbars.",
                4.7, "Mar–Jun", "3 days, 2 nights", "Easy", "₱6,000–11,000 per person",
                "Island hopping;Hot spring bathing;Beach exploration;Sunken cemetery sightseeing;Waterfall exploration",
                "White Island|Sandbar with views of Mount Hibok-Hibok;Sunken Cemetery|Marker and reef in the sea;Katibawasan Falls|Tall waterfall with a cool pool");
    }

    // ---------- helper used by the seed methods ----------
    private Destination add(String region, String name, String province, String categories,
                            String description, double score, String bestTime, String duration,
                            String difficulty, String budget, String activities, String places) {

        Destination d = new Destination(name, province, region,
                Arrays.asList(categories.split(",")), description);
        d.setScore(score);
        d.setTravelInfo(bestTime, duration, difficulty, budget);
        d.setActivities(Arrays.asList(activities.split(";")));
        d.setPlaces(Arrays.asList(places.split(";")));
        destinations.add(d);
        return d;
    }
}