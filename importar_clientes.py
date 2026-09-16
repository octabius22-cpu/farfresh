import csv
import firebase_admin
import os
from firebase_admin import credentials, firestore

# 1. Configuración de Rutas Absolutas para evitar errores en Windows
BASE_DIR = "D:/FAR/farfresh"
KEY_FILE = os.path.join(BASE_DIR, "farfre-firebase-adminsdk-fbsvc-99950b8009.json")
CSV_FILE = os.path.join(BASE_DIR, "clientes.csv")

try:
    print(f"Buscando llave en: {KEY_FILE}")
    if not os.path.exists(KEY_FILE):
        raise FileNotFoundError(f"No se encuentra el archivo .json en {KEY_FILE}")

    cred = credentials.Certificate(KEY_FILE)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    print("✅ Conexión con Firebase establecida correctamente.")
except Exception as e:
    print(f"❌ ERROR DE CONFIGURACIÓN: {e}")
    exit()

def importar_csv(file_path):
    coleccion = db.collection('customers')

    try:
        print(f"Buscando CSV en: {file_path}")
        with open(file_path, mode='r', encoding='utf-8-sig') as csv_file:
            content = csv_file.read(2048)
            csv_file.seek(0)
            dialect = csv.Sniffer().sniff(content)
            reader = csv.DictReader(csv_file, dialect=dialect)

            batch = db.batch()
            count = 0
            total = 0

            print(">>> Iniciando subida masiva a la nube...")

            for row in reader:
                codigo = str(row.get('CODIGO', '')).strip()
                nombre = str(row.get('APELLIDOS Y NOMBRES', '')).strip()
                dni = str(row.get('DNI', '')).strip()

                if not codigo or not nombre:
                    continue

                doc_ref = coleccion.document(codigo)
                data = {
                    "id": codigo,
                    "name": nombre,
                    "dni": dni,
                    "phone": None,
                    "isActive": True,
                    "createdAt": firestore.SERVER_TIMESTAMP
                }

                batch.set(doc_ref, data, merge=True)
                count += 1
                total += 1

                if count == 500:
                    batch.commit()
                    print(f"✅ {total} registros subidos...")
                    batch = db.batch()
                    count = 0

            if count > 0:
                batch.commit()

            print(f"\n🚀 ¡ÉXITO TOTAL! Se importaron {total} clientes correctamente.")

    except Exception as e:
        print(f"❌ ERROR DURANTE LA IMPORTACIÓN: {e}")

if __name__ == "__main__":
    importar_csv(CSV_FILE)
