import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import time

# Using a high-capacity alternative Overpass mirror to avoid the previous limits
OVERPASS_URL = "https://maps.mail.ru/osm/tools/overpass/api/interpreter"

# Only the countries that failed or timed out
FAILED_COUNTRIES = ["EG", "QA", "LB", "JO", "KW", "IQ"]

# We break the massive query into 4 smaller chunks so we don't crash the server memory
QUERY_CHUNKS = [
    'node["shop"]',
    'way["shop"]',
    'node["office"]',
    'way["office"]'
]

def recover_missing_countries():
    session = requests.Session()
    retries = Retry(total=5, backoff_factor=3, status_forcelist=[500, 502, 503, 504, 429])
    session.mount('https://', HTTPAdapter(max_retries=retries))
    
    headers = {
        "User-Agent": "MENA-JobBoard-DataScanner/3.0",
        "Accept": "application/json"
    }

    print("🚀 Starting the Recovery Extractor for Missing Countries...\n")

    total_recovered = 0

    for country_code in FAILED_COUNTRIES:
        country_companies = set()
        print(f"📡 Processing {country_code} in smaller chunks...")
        
        for chunk in QUERY_CHUNKS:
            print(f"   -> Extracting {chunk} data for {country_code}...")
            
            query = f"""
            [out:json][timeout:900];
            area["ISO3166-1"="{country_code}"][admin_level=2]->.searchArea;
            (
              {chunk}(area.searchArea);
            );
            out tags;
            """
            
            try:
                response = session.post(OVERPASS_URL, data={'data': query}, headers=headers, timeout=120)
                
                if response.status_code == 200:
                    data = response.json()
                    for elem in data.get("elements", []):
                        tags = elem.get("tags", {})
                        name = tags.get("name:en") or tags.get("name")
                        
                        if name:
                            clean = name.strip()
                            if len(clean) > 2 and not clean.isnumeric():
                                country_companies.add(clean)
                else:
                    print(f"   ⚠️ Server returned {response.status_code} for {chunk}")
                
                # Rest the server between chunks
                time.sleep(5)
                
            except Exception as e:
                print(f"   ❌ Failed on {chunk} for {country_code}: {e}")
                time.sleep(5)

        count = len(country_companies)
        print(f"✅ Recovered {count} unique businesses for {country_code}!")
        
        # Append the recovered data to your existing master file
        if count > 0:
            with open("raw_me_companies.txt", "a", encoding="utf-8") as f:
                for company in sorted(country_companies):
                    f.write(f"{company}\n")
            total_recovered += count
            print(f"💾 Safely appended {country_code} to 'raw_me_companies.txt'.\n")

    print(f"🎉 Recovery Complete! Added {total_recovered} new businesses to your master list.")

if __name__ == "__main__":
    recover_missing_countries()