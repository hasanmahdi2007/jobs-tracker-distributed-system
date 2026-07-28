# Load, deduplicate, sort, and save
with open("raw_me_companies.txt", "r", encoding="utf-8") as f:
    # A set automatically deletes identical duplicates
    unique_companies = sorted(set(line.strip() for line in f if line.strip()))

with open("raw_me_companies.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(unique_companies) + "\n")

print(f"✅ Cleaned and sorted! Final total unique companies: {len(unique_companies)}")