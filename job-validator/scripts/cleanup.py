import re

# The 41 confirmed tokens we want to purge from our master list
CONFIRMED_TALENTERA_TOKENS = {
    "aab", "tra", "royaljordanian", "mobica", "masar", "tamimi", "mobily",
    "orangebedbath", "magrabi", "rawabi", "itqan", "chi", "radwa", "careers",
    "integral", "ncec", "panda", "beam", "care", "badruddin", "saib", "gosi",
    "al-dawaa", "savethechildren", "taiba", "concrete", "maliks", "ejada",
    "demo", "sba", "choithrams", "alfardan", "gib", "msd", "sal", "spsp",
    "batelco", "nwc", "cma", "modon", "alrashed"
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

print("🧹 Sweeping 80,000+ line master list for found companies...")

# Read your original list
with open("../data/raw_me_companies.txt", "r", encoding="utf-8") as f:
    for line in f:
        company = line.strip()
        if not company:
            continue
            
        slugs = generate_slugs(company)
        
        if slugs.intersection(CONFIRMED_TALENTERA_TOKENS):
            removed_count += 1
        else:
            kept_companies.append(company)

# Write the remaining companies to a new, clean file
with open("../data/raw_me_companies_filtered.txt", "w", encoding="utf-8") as f:
    for company in kept_companies:
        f.write(company + "\n")

print(f"✅ Cleanup Complete! Removed {removed_count} companies.")