import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import os
from git import Repo

repo_path = "database-repo"

# # Clone the GitHub repository
# repo_url = "https://github.com/iptv-org/database.git"
# if os.path.exists(repo_path):
#     # Delete the existing repository directory
#     import shutil
#     shutil.rmtree(repo_path)

# # Clone the repository
# print("Cloning the GitHub repository...")
# repo = Repo.clone_from(repo_url, repo_path)
# print("Repository cloned successfully!")


# Initialize the Firestore client
cred = credentials.Certificate('firevisioniptv-firebase-adminsdk-n372z-e166f55b1c.json')
firebase_admin.initialize_app(cred, {
    'projectId': 'firevisioniptv',
})
db = firestore.client()




# Function to process the CSV data and upload to Firestore
def upload_data_to_firestore():
    # Upload countries
    # csv_file = os.path.join(repo_path, "data/countries.csv")
    # with open(csv_file, 'r') as file:
    #     lines = file.readlines()
    #     header = lines[0].strip().split(',')

    #     for i, line in enumerate(lines[1:]):
    #         country_data = line.strip().split(',')
    #         country_dict = {header[j]: country_data[j] for j in range(len(header))}
    #         doc_ref = db.collection('countries').document(country_dict['code'])
    #         doc_ref.set(country_dict)
    #         print(f"Uploaded data for {country_dict['name']} ({country_dict['code']})")

    # # Upload categories
    # csv_file = os.path.join(repo_path, "data/categories.csv")
    # with open(csv_file, 'r') as file:
    #     lines = file.readlines()
    #     header = lines[0].strip().split(',')

    #     for i, line in enumerate(lines[1:]):
    #         category_data = line.strip().split(',')
    #         category_dict = {header[j]: category_data[j] for j in range(len(header))}
    #         doc_ref = db.collection('categories').document(category_dict['id'])
    #         doc_ref.set(category_dict)
    #         print(f"Uploaded category: {category_dict['name']}")

    # Upload channels
    csv_file = os.path.join(repo_path, "data/channels.csv")
    with open(csv_file, 'r') as file:
        lines = file.readlines()
        header = lines[0].strip().split(',')

        for i, line in enumerate(lines[1:]):
            channel_data = line.strip().split(',')
            channel_dict = {header[j]: channel_data[j] for j in range(len(header))}
            doc_ref = db.collection('channels').document(channel_dict['id'])
            doc_ref.set(channel_dict)
            print(f"Uploaded channel: {channel_dict['name']}")

    # Upload languages
    csv_file = os.path.join(repo_path, "data/languages.csv")
    with open(csv_file, 'r') as file:
        lines = file.readlines()
        header = lines[0].strip().split(',')

        for i, line in enumerate(lines[1:]):
            language_data = line.strip().split(',')
            language_dict = {header[j]: language_data[j] for j in range(len(header))}
            doc_ref = db.collection('languages').document(language_dict['code'])
            doc_ref.set(language_dict)
            print(f"Uploaded language: {language_dict['name']}")

    # Upload regions
    csv_file = os.path.join(repo_path, "data/regions.csv")
    with open(csv_file, 'r') as file:
        lines = file.readlines()
        header = lines[0].strip().split(',')

        for i, line in enumerate(lines[1:]):
            region_data = line.strip().split(',')
            region_dict = {header[j]: region_data[j] for j in range(len(header))}
            doc_ref = db.collection('regions').document(region_dict['code'])
            doc_ref.set(region_dict)
            print(f"Uploaded region: {region_dict['name']}")

    # Upload subdivisions
    csv_file = os.path.join(repo_path, "data/subdivisions.csv")
    with open(csv_file, 'r') as file:
        lines = file.readlines()
        header = lines[0].strip().split(',')

        for i, line in enumerate(lines[1:]):
            subdivision_data = line.strip().split(',')
            subdivision_dict = {header[j]: subdivision_data[j] for j in range(len(header))}
            doc_ref = db.collection('subdivisions').document(f"{subdivision_dict['country']}-{subdivision_dict['code']}")
            doc_ref.set(subdivision_dict)
            print(f"Uploaded subdivision: {subdivision_dict['name']} ({subdivision_dict['code']})")


# Call the function to upload the data



upload_data_to_firestore()