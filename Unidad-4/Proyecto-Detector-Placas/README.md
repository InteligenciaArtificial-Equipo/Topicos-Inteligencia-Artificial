# Proyecto Unidad 4
### App Movil para detectar placas de coches y saber el propietario.

## Integrantes
- Luis Antonio Peñuelas López
- Peraza Medina Eliezer Daniel

## Acerca del proyecto
El proyecto consiste en un sistema detector de placas utilizando un modelo de vision artificial.
Se debe analizar la placa y mostrar al usuario el propietario.

### Base de Datos

La base de datos se desarrollo en *postgres*, la cual cuenta con 2 tablas:
- Owners
- Plates

Los datos que almacenan las tablas son minimos siendo para owners solo el nombre y en el caso de plates la placa y el id del propietario.

### Modelo

- OCR con EasyOCR y Tesseract
- Preprocesamiento de imágenes
- Limpieza de texto

Adicional se realizo un entrenamiento de un modelo preentrenado de YOLO con un dataset propio se adjunta resultados de entrenamiento, sin embargo al momento de su implementacion y prueba en la app los resultados dejaban mucho que desear por yo se opto de uso de un OCR con EasyOCR, Nos hubiera encando usar un modelo propio pero nuestros equipos no cuentan con la potencia suficiente para realizar tantos entrenamientos al no contar con una GPU el proceso fue muy lento.

<img width="2400" height="1200" alt="image" src="https://github.com/user-attachments/assets/97a40947-fe69-4581-9b03-1af2cff62417" />

#### recognize_plate.py

Este script en *Python* implementa un sistema avanzado de Reconocimiento Óptico de Caracteres (OCR) para matrículas de vehículos. 
Utiliza una estrategia multimétodo, combinando múltiples técnicas de preprocesamiento de imágenes con el uso de las bibliotecas EasyOCR y Tesseract, para maximizar la tasa de éxito en la lectura de placas bajo diversas condiciones de imagen.

Este es el script usado en el backend para responder las peticiones de consulta.

- Doble OCR: Soporte para EasyOCR y Pytesseract para redundancia y mejor precisión.
- Limpieza y Validación: Limpia el texto reconocido (mayúsculas, eliminación de caracteres especiales) y valida el formato para asegurar que el resultado sea una placa plausible.
- Selección por Confianza: Prioriza los resultados basándose en la puntuación de confianza del OCR y reporta los principales candidatos.

### BackEnd

El backend en Scala3:

- API REST con 2 endpoints (http4s)
  - Registrar placas y propietarios, recibe la placa y el nombre del propietario y hace el registro.
  - Consultar propietario por placa, recibe la imagen de la placa en base-64
- Integrado con *postgres*
- CORS habilitado
- Manego de imagenes
- Ejecucion del modelo de vision artificial

#### Estructura Modular

Main.scala
models
- Owner.scala
- Plate.scala
- PlateWithOwner.scala
daos
- Database.scala
helpers
- PlateRecognition.scala
routes
- Requests.scala
- Responses.scala
- Routes.scala

#### Ejecucion

> ``` sbt run```

### App Mobil (JS y React Native)

- 2 pantallas con navegación
  - Consultar y Registrar
- Intefaz sencilla e intuitiva

#### Estrutura Modular

- App.js
- config.js
- index.js
- styles.js
- services
  - api.js
- screens
  - QueryScreen.js
  - RegisterScreen.js

#### Ejecucion (Requiere la app movil Expo Go durante la fase de desarrollo y pruebas)

> ```  npx expo start --tunnel ```

#### Ejemplos de Ejecucion

##### Ejemplo 1

- **Registro**

<img width="716" height="1600" alt="image" src="https://github.com/user-attachments/assets/dd9ef1d3-d6d4-4859-9269-423a1ffbe9d8" />


**Consulta**

<img width="716" height="1600" alt="image" src="https://github.com/user-attachments/assets/ba0e820b-6a55-4f41-b38a-a4875945ac55" />

##### Ejemplo 2

- **Registro**

<img width="716" height="1600" alt="image" src="https://github.com/user-attachments/assets/454552e9-7ce8-49bc-ae3a-107c6d01d052" />


**Consulta**

<img width="716" height="1600" alt="image" src="https://github.com/user-attachments/assets/84c53440-4b8a-48b7-bfe1-1b3aa1e0432b" />


##### Ejemplo 3

- **Registro**

<img width="716" height="1600" alt="image" src="https://github.com/user-attachments/assets/f74e13ab-9f2d-45e2-b4fc-31d1877fa5bd" />


**Consulta**

<img width="716" height="1600" alt="image" src="https://github.com/user-attachments/assets/e783e506-90ed-4b0c-829b-fbe2de48222e" />


