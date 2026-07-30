import re

# Confirmed Greenhouse tokens

# Confirmed Lever tokens
CONFIRMED_LEVER_TOKENS = {
    "mirror", "spectrum", "isee", "grand", "ashoka", "aero", "vana", "cic",
    "falcon", "tonic", "form", "aleph", "better", "ion", "incorta", "florence",
    "sure", "accurate", "tala", "sila", "career", "sar", "life", "mazars",
    "horizon", "maya", "source", "nextech", "greenlight", "blue", "raya",
    "erg", "hotstar", "coins", "retro", "aldar", "advocate", "pattern",
    "fantasy", "rise", "gamma", "rai", "kpmg", "reliable", "trio", "fortress",
    "powerex", "bigblue", "angel", "neon", "avante", "abt", "gate", "moo",
    "capital", "tag", "lever", "beta", "sep", "assurance", "harmony",
    "dutch", "text", "lemon", "safe", "terminus", "jmi", "crossfit",
    "aquarium", "charcuterie", "adora", "mega", "signal", "espace", "relay",
    "sfeir", "deputy", "mawsim", "ans", "brilliant", "computercare"
}

# Merge all confirmed tokens together
CONFIRMED_PURGE_TOKENS = CONFIRMED_LEVER_TOKENS

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

print(f"🧹 Sweeping master list against {len(CONFIRMED_PURGE_TOKENS)} Greenhouse & Lever tokens...")

with open("../data/raw_me_companies.txt", "r", encoding="utf-8") as f:
    for line in f:
        company = line.strip()
        if not company:
            continue
            
        slugs = generate_slugs(company)
        
        if slugs.intersection(CONFIRMED_PURGE_TOKENS):
            removed_count += 1
        else:
            kept_companies.append(company)

with open("../data/raw_me_companies_filtered.txt", "w", encoding="utf-8") as f:
    for company in kept_companies:
        f.write(company + "\n")

print(f"✅ Cleanup Complete! Removed {removed_count} matched companies.")