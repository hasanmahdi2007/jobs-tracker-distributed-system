import re

# Keep your hardcoded Lever/Greenhouse tokens here
CONFIRMED_LEVER_TOKENS = {
    "mirror", "spectrum", "isee", "grand", "ashoka", "aero", "vana", "cic",
    "falcon", "tonic", "form", "aleph", "better", "ion", "incorta", "florence",
    # ... (the rest of your hardcoded tokens)
    "sfeir", "deputy", "mawsim", "ans", "brilliant", "computercare"
}

# 1. Dynamically load the 6,000+ SmartRecruiters tokens you just pulled from Redis
with open("smartrecruiters_slugs.txt", "r", encoding="utf-8") as f:
    smartrecruiters_tokens = set(line.strip() for line in f if line.strip())

# 2. Merge them together using the union() operator
CONFIRMED_PURGE_TOKENS = CONFIRMED_LEVER_TOKENS.union(smartrecruiters_tokens)

print(f"Loaded {len(smartrecruiters_tokens)} SmartRecruiters tokens.")
print(f"Total tokens in master purge list: {len(CONFIRMED_PURGE_TOKENS)}")

def generate_slugs(raw_name):
    # 1. Lowercase
    clean = raw_name.lower()
    
    # ... (the rest of your script remains exactly the same)