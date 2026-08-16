import zipfile
import pandas as pd
import redis
import re

# Connect to local Redis instance
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

# Target Redis queue key mapped to the bamboohrThrottledRunner
REDIS_QUEUE_KEY = "queue:slugs:bamboohr"

# Your target zip file path
ZIP_PATH = r"C:\Users\user\Downloads\companies-2023-q4-sm.csv.zip"

def clean_to_slugs(raw_name):
    clean = re.sub(r'[\u0600-\u06FF]', '', str(raw_name).lower())
    clean = re.sub(r'\b(llc|ltd|co|inc|sal|wll|fzco|fz-llc|group|company|corporation|llp)\b', '', clean)
    clean = re.sub(r'[^a-z0-9\s]', ' ', clean)
    clean = re.sub(r'\b\d+\b', '', clean)
    clean = ' '.join(clean.split()).strip()
    
    if not clean or len(clean) < 3:
        return set()

    slugs = set()
    dashed = clean.replace(" ", "-")
    mashed = clean.replace(" ", "")
    
    if 3 < len(dashed) < 63: slugs.add(dashed)
    if 3 < len(mashed) < 63: slugs.add(mashed)
    return slugs

print(f"📦 Opening zip file: {ZIP_PATH}")

with zipfile.ZipFile(ZIP_PATH, 'r') as z:
    csv_filename = z.namelist()[0]
    print(f"📄 Found inside: {csv_filename}")
    
    with z.open(csv_filename) as f:
        # Detect exact column name (handles 'name', 'Name', etc.)
        first_line = pd.read_csv(f, nrows=1)
        f.seek(0)
        
        name_col = next((col for col in ['name', 'Name', 'company_name', 'organization'] if col in first_line.columns), first_line.columns[0])
        print(f"🔍 Reading company names from column: '{name_col}'")

        chunk_size = 100_000
        total_slugs = 0
        
        for chunk in pd.read_csv(f, chunksize=chunk_size, usecols=[name_col], dtype=str):
            slug_batch = set()
            for name in chunk[name_col].dropna():
                slugs = clean_to_slugs(name)
                slug_batch.update(slugs)
            
            if slug_batch:
                r.sadd(REDIS_QUEUE_KEY, *slug_batch)
                total_slugs += len(slug_batch)
                print(f"📥 Chunk processed. Total unique candidate slugs in Redis: {total_slugs:,}")

print(f"✅ Finished! Set '{REDIS_QUEUE_KEY}' is ready for your Spring Boot validator.")