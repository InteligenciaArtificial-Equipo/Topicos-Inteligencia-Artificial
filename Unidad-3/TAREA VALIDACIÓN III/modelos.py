# -----------------------------------------------------------
# modelos.py
# Contiene las clases principales del modelo de datos:
# - Municipio: representa una ciudad o punto con coordenadas
# - Aptitud: calcula la distancia total y la calidad (fitness)
# -----------------------------------------------------------

import numpy as np

class Municipio:
    def __init__(self, x, y, nombre=None):
        # Coordenadas del municipio
        self.x = x
        self.y = y
        # Nombre opcional
        self.nombre = nombre

    # Calcula la distancia euclídea entre este municipio y otro
    def distancia(self, otro):
        return np.sqrt((self.x - otro.x)**2 + (self.y - otro.y)**2)

    # Representación legible del municipio al imprimirlo
    def __repr__(self):
        return f"{self.nombre or ''}({self.x:.2f},{self.y:.2f})"


class Aptitud:
    def __init__(self, ruta):
        # Lista de municipios que forman la ruta
        self.ruta = ruta
        # Distancia total y valor de aptitud (fitness)
        self.distancia = 0
        self.f_aptitud = 0.0

    # Calcula la distancia total de la ruta (vuelta al origen incluida)
    def distancia_ruta(self):
        if self.distancia == 0:
            for i in range(len(self.ruta)):
                a = self.ruta[i]
                b = self.ruta[(i + 1) % len(self.ruta)]
                self.distancia += a.distancia(b)
        return self.distancia

    # Calcula la aptitud (inverso de la distancia)
    def ruta_apta(self):
        if self.f_aptitud == 0:
            dist = self.distancia_ruta()
            self.f_aptitud = 1 / dist if dist != 0 else 0
        return self.f_aptitud
