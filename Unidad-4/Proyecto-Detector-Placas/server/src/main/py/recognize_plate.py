#!/usr/bin/env python3
"""
Reconocimiento de placas mejorado con múltiples métodos
"""

import sys
import cv2
import numpy as np
import re

# Intentar diferentes métodos de OCR
USE_EASYOCR = True
USE_TESSERACT = True

try:
    import easyocr
except ImportError:
    USE_EASYOCR = False
    print("EasyOCR no disponible", file=sys.stderr)

try:
    import pytesseract
except ImportError:
    USE_TESSERACT = False
    print("Tesseract no disponible", file=sys.stderr)


class PlateRecognizer:
    def __init__(self):
        self.readers = []
        
        # Inicializar EasyOCR si está disponible
        if USE_EASYOCR:
            try:
                self.easy_reader = easyocr.Reader(['en'], gpu=False)
                self.readers.append('easyocr')
            except Exception as e:
                print(f"Error inicializando EasyOCR: {e}", file=sys.stderr)
        
        # Tesseract está disponible?
        if USE_TESSERACT:
            self.readers.append('tesseract')
    
    def preprocess_image(self, image_path):
        """Preprocesar imagen con múltiples técnicas"""
        img = cv2.imread(image_path)
        if img is None:
            return None, None
        
        # Redimensionar si es muy grande
        height, width = img.shape[:2]
        if width > 1000:
            scale = 1000 / width
            img = cv2.resize(img, None, fx=scale, fy=scale)
        
        # Convertir a escala de grises
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        
        # Múltiples preprocesos
        processed_images = []
        
        # 1. Original en gris
        processed_images.append(('original', gray))
        
        # 2. Threshold binario
        _, thresh1 = cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)
        processed_images.append(('binary', thresh1))
        
        # 3. Threshold adaptativo
        thresh2 = cv2.adaptiveThreshold(
            gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
            cv2.THRESH_BINARY, 11, 2
        )
        processed_images.append(('adaptive', thresh2))
        
        # 4. OTSU
        _, thresh3 = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        processed_images.append(('otsu', thresh3))
        
        # 5. Bilateral filter + threshold
        bilateral = cv2.bilateralFilter(gray, 11, 17, 17)
        _, thresh4 = cv2.threshold(bilateral, 127, 255, cv2.THRESH_BINARY)
        processed_images.append(('bilateral', thresh4))
        
        # 6. Dilate + Erode
        kernel = np.ones((3,3), np.uint8)
        dilated = cv2.dilate(gray, kernel, iterations=1)
        eroded = cv2.erode(dilated, kernel, iterations=1)
        processed_images.append(('morph', eroded))
        
        # 7. Aumentar contraste
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
        contrast = clahe.apply(gray)
        processed_images.append(('contrast', contrast))
        
        return img, processed_images
    
    def clean_plate_text(self, text):
        """Limpiar y validar texto de placa"""
        if not text:
            return None
        
        # Convertir a mayúsculas y eliminar espacios
        cleaned = text.upper().strip()
        
        # Remover caracteres especiales pero mantener letras y números
        cleaned = re.sub(r'[^A-Z0-9]', '', cleaned)
        
        # Validar formato mínimo (al menos 5 caracteres, con letras y números)
        if len(cleaned) < 5:
            return None
        
        has_letters = any(c.isalpha() for c in cleaned)
        has_numbers = any(c.isdigit() for c in cleaned)
        
        if not (has_letters and has_numbers):
            return None
        
        # Tomar primeros 6-7 caracteres (formato típico de placas)
        if len(cleaned) > 7:
            cleaned = cleaned[:7]
        
        return cleaned
    
    def recognize_with_easyocr(self, processed_images):
        """Reconocer con EasyOCR"""
        results = []
        
        for name, img in processed_images:
            try:
                detections = self.easy_reader.readtext(img)
                for (bbox, text, confidence) in detections:
                    cleaned = self.clean_plate_text(text)
                    if cleaned and confidence > 0.3:
                        results.append((cleaned, confidence, name))
            except Exception as e:
                print(f"Error EasyOCR en {name}: {e}", file=sys.stderr)
        
        return results
    
    def recognize_with_tesseract(self, processed_images):
        """Reconocer con Tesseract"""
        results = []
        
        # Configuración para placas (solo alfanumérico)
        config = '--psm 7 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
        
        for name, img in processed_images:
            try:
                text = pytesseract.image_to_string(img, config=config)
                cleaned = self.clean_plate_text(text)
                if cleaned:
                    # Tesseract no da confidence directamente, usamos 0.5
                    results.append((cleaned, 0.5, name))
            except Exception as e:
                print(f"Error Tesseract en {name}: {e}", file=sys.stderr)
        
        return results
    
    def recognize(self, image_path):
        """Reconocer placa usando todos los métodos disponibles"""
        print(f"Procesando: {image_path}", file=sys.stderr)
        
        # Preprocesar
        original, processed_images = self.preprocess_image(image_path)
        
        if processed_images is None:
            print("Error cargando imagen", file=sys.stderr)
            return None
        
        all_results = []
        
        # Intentar con EasyOCR
        if 'easyocr' in self.readers:
            print("Intentando con EasyOCR...", file=sys.stderr)
            easy_results = self.recognize_with_easyocr(processed_images)
            all_results.extend(easy_results)
            print(f"EasyOCR encontró: {len(easy_results)} candidatos", file=sys.stderr)
        
        # Intentar con Tesseract
        if 'tesseract' in self.readers:
            print("Intentando con Tesseract...", file=sys.stderr)
            tess_results = self.recognize_with_tesseract(processed_images)
            all_results.extend(tess_results)
            print(f"Tesseract encontró: {len(tess_results)} candidatos", file=sys.stderr)
        
        if not all_results:
            print("No se encontraron placas", file=sys.stderr)
            return None
        
        # Ordenar por confianza
        all_results.sort(key=lambda x: x[1], reverse=True)
        
        # Mostrar top 3 candidatos
        print("\nTop candidatos:", file=sys.stderr)
        for i, (text, conf, method) in enumerate(all_results[:3]):
            print(f"{i+1}. {text} (conf: {conf:.2f}, método: {method})", file=sys.stderr)
        
        # Retornar el mejor
        best_match = all_results[0][0]
        print(f"\nMejor match: {best_match}", file=sys.stderr)
        
        return best_match


def main():
    if len(sys.argv) < 2:
        print("Uso: python recognize_plate.py <ruta_imagen>", file=sys.stderr)
        sys.exit(1)
    
    image_path = sys.argv[1]
    
    recognizer = PlateRecognizer()
    
    if not recognizer.readers:
        print("ERROR: No hay métodos de OCR disponibles", file=sys.stderr)
        print("Instala: pip install easyocr pytesseract", file=sys.stderr)
        sys.exit(1)
    
    result = recognizer.recognize(image_path)
    
    if result:
        print(result)  # Esto es lo que captura Scala
    else:
        print("", end="")
        sys.exit(1)


if __name__ == "__main__":
    main()