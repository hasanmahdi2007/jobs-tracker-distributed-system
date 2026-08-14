import os

# Updated paths to read from and write to the data folder
INPUT_FILE = "../data/raw_slugs.txt"
YAML_OUTPUT = "../data/smartrecruiters_boards.yml"
SQL_OUTPUT = "../data/smartrecruiters_inserts.sql"

def process_tokens():
    # FIXED: Changed encoding to utf-16 to handle PowerShell text dumps
    with open(INPUT_FILE, "r", encoding="utf-16") as f:
        tokens = [line.strip() for line in f if line.strip()]

    if not tokens:
        print("Error: No tokens found in input file.")
        return

    # 1. Generate the YAML Output (Saving as utf-8 so Spring Boot reads it perfectly)
    with open(YAML_OUTPUT, "w", encoding="utf-8") as yml:
        yml.write("smartrecruiters:\n")
        yml.write("      base-url: \"https://api.smartrecruiters.com/v1/companies\"\n")
        yml.write("      target-boards:\n")
        for token in tokens:
            yml.write(f"        - {token}\n")

    # 2. Generate the SQL Output (Saving as utf-8 so Postgres reads it perfectly)
    with open(SQL_OUTPUT, "w", encoding="utf-8") as sql:
        sql_lines = []
        for token in tokens:
            company_name = token.capitalize()
            sql_lines.append(f"('{company_name}', 'SMARTRECRUITERS', '{token}', 'https://jobs.smartrecruiters.com/{token}')")
        
        sql_content = ",\n".join(sql_lines)
        sql_content += "\nON CONFLICT (board_token) DO NOTHING;\n"
        
        sql.write(sql_content)

    print(f"✅ Successfully processed {len(tokens)} tokens!")
    print(f"📁 Created YAML file: {YAML_OUTPUT}")
    print(f"📁 Created SQL file: {SQL_OUTPUT}")

if __name__ == "__main__":
    process_tokens()