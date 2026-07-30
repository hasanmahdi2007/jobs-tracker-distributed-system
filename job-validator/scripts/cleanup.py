import re

# Confirmed Greenhouse tokens to purge
CONFIRMED_GREENHOUSE_TOKENS = {
    "gps", "london", "orca", "spotlight", "kayak", "national", "alx", "gemini",
    "concentric", "magnolia", "stage", "galileo", "raven", "super", "digit",
    "apollo", "tulip", "pvi", "denver", "chicago", "tide", "flourish", "bob",
    "brothers", "taa", "regent", "nex", "abc", "flamingo", "glow", "reflex",
    "korea", "its", "coast", "indigo", "metropolis", "oldcity", "paradigm",
    "alabaster", "octave", "reliant", "headquarter", "lightworks", "bloom",
    "rak", "shield", "upkeep", "truemedia", "soci", "dna", "administrative",
    "athletics", "sei", "metro", "athena", "support", "hunterdouglas", "zoro",
    "carbon", "greenline", "pioneer", "yes", "avani", "help", "eclipse",
    "jaguar", "dino", "koko", "rmr", "new", "sao", "tomorrow", "stone", "seed",
    "iris", "comet", "investment", "ghost", "casa", "nursing", "shaker",
    "encore", "range", "ref", "moneymart", "excel", "optimal", "gateway",
    "reach", "slice", "forward", "fox", "isa", "murad", "sunrise", "sada",
    "pine", "charles", "proton", "clever", "sts", "private", "netherlands",
    "hala", "amca", "truckstop", "moon", "boulevard", "parkland", "axiom",
    "newhope", "sonic", "techno", "genuine", "careem", "tribal", "drc",
    "homeland", "vibes", "mesh", "chata", "cabin", "kansas", "polaris", "peak",
    "magic", "oscar", "mobi", "broadway", "efs", "sterling", "integrated",
    "isam", "link", "evergreen", "ideo", "candles", "flex", "gusto", "maxwell",
    "make", "industrial", "mit", "wundermanthompson", "talent", "independent",
    "makina", "david", "tcs", "firstday", "calm", "sas", "victory", "emi",
    "nexus", "regie", "solutions", "b12", "dallas", "sabino", "kit", "japan",
    "antigua", "imc", "testing", "elitetechnology", "general", "gravity",
    "tss", "sunset", "jay", "scanner", "workshop", "explore", "flix", "space",
    "universal", "journey", "tec", "galaxy", "branch", "echo", "location",
    "shadow", "edison", "tiptop", "philippines", "name", "mas", "res", "tpm",
    "lighthouse", "champion", "ensemble", "bold", "bridgeway", "clear", "any",
    "newton", "cts", "joya", "finance", "axon", "aziz", "symphony", "aura",
    "coop", "gmp", "future", "locals", "orchestra", "cycles", "contracts",
    "bees", "shepherd", "daylight", "hologram", "pharmacy", "ultimate", "vaya",
    "engine", "intercom", "events", "find", "test", "keen", "public", "highland",
    "sra", "forte", "disney", "lor", "flash", "berelaxspa", "air", "cannondale",
    "silicon", "fourstar", "prisma", "international", "beyond", "cais",
    "colorado", "spain", "zoo", "genius", "aha", "village", "nectar", "lush",
    "supreme", "transform", "alliance", "festival", "apex", "kite", "rga",
    "icon", "rab", "esri", "basic", "place", "allied", "blend", "selling",
    "osmosis", "goal", "map", "summer", "alta", "maro", "fourseasons", "elite",
    "ampm", "maestro", "via", "ohio", "a24", "alu", "technology", "glance",
    "tekla", "neu", "system", "insurance", "education", "harbor", "moltonbrown",
    "uplift", "pia", "network", "grey", "lasenza", "doc", "vmax", "pronto",
    "community", "cleo", "trolley", "route", "tamara", "pcm", "radar",
    "crescent", "impact", "remedy", "action", "axle", "eve", "wasabi", "zam",
    "rehabilitation", "goodwin", "trident", "bird", "honor", "inter", "block",
    "cherry", "fruitful", "pulse", "cio", "edo", "mercury", "bravo", "jumia",
    "noble", "retail", "ever", "nice", "markets", "emerge", "tca", "quince",
    "des", "golf", "genesis", "technical", "onyx", "squad", "purple", "scotch",
    "shein", "essential", "lincoln", "escada", "india", "oasis", "escala",
    "vts", "petcare", "vega", "claros", "remote", "byd", "firstchoice", "camp",
    "greenhouse"
}

def generate_slugs(raw_name):
    clean = re.sub(r'[\u0600-\u06FF]', '', raw_name.lower())
    clean = re.sub(r'\b(llc|ltd|co|inc|sal|wll|fzco|fz-llc|group|company|corporation|llp)\b', '', clean)
    clean = re.sub(r'[^a-z0-9\s]', ' ', clean)
    clean = re.sub(r'\b\d+\b', '', clean)
    clean = ' '.join(clean.split()).strip()
    
    if not clean or len(clean) < 3:
        return set()

    slugs = set()
    dashed = clean.replace(" ", "-")
    mashed = clean.replace(" ", "")
    
    if 2 < len(dashed) < 63: slugs.add(dashed)
    if 2 < len(mashed) < 63: slugs.add(mashed)
    if " " in clean:
        first_word = clean.split(" ")[0]
        if len(first_word) >= 3: slugs.add(first_word)

    return slugs

kept_companies = []
removed_count = 0

print(f"🧹 Sweeping master list against {len(CONFIRMED_GREENHOUSE_TOKENS)} Greenhouse tokens...")

with open("../data/raw_me_companies.txt", "r", encoding="utf-8") as f:
    for line in f:
        company = line.strip()
        if not company:
            continue
            
        slugs = generate_slugs(company)
        
        if slugs.intersection(CONFIRMED_GREENHOUSE_TOKENS):
            removed_count += 1
        else:
            kept_companies.append(company)

with open("../data/raw_me_companies_filtered.txt", "w", encoding="utf-8") as f:
    for company in kept_companies:
        f.write(company + "\n")

print(f"✅ Cleanup Complete! Removed {removed_count} Greenhouse companies.")