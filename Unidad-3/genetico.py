# -----------------------------------------------------------
# genetico.py
# Contiene las funciones del algoritmo genético:
# creación de rutas, selección, reproducción, mutación y evolución
# -----------------------------------------------------------

import random
import operator
import pandas as pd
from modelos import Aptitud

# ---------------------- Creación de población ----------------------
def crear_ruta(lista_municipios):
    # Crea una ruta aleatoria con los municipios dados
    return random.sample(lista_municipios, len(lista_municipios))

def poblacion_inicial(tamano, lista_municipios):
    # Genera una lista de rutas aleatorias (población inicial)
    return [crear_ruta(lista_municipios) for _ in range(tamano)]

# ---------------------- Evaluación ----------------------
def clasificar_rutas(poblacion):
    # Evalúa cada ruta y la ordena por su aptitud (de mejor a peor)
    resultados = {i: Aptitud(p).ruta_apta() for i, p in enumerate(poblacion)}
    return sorted(resultados.items(), key=operator.itemgetter(1), reverse=True)

# ---------------------- Selección ----------------------
def seleccion_rutas(pop_ranked, num_elite):
    # Mantiene la élite y selecciona el resto por ruleta
    df = pd.DataFrame(pop_ranked, columns=["Indice", "Aptitud"])
    df["cum_sum"] = df.Aptitud.cumsum()
    df["cum_perc"] = 100 * df.cum_sum / df.Aptitud.sum()

    # Mantener los mejores individuos
    seleccionados = [pop_ranked[i][0] for i in range(num_elite)]

    # Selección por ruleta
    for _ in range(len(pop_ranked) - num_elite):
        pick = 100 * random.random()
        for i in range(len(pop_ranked)):
            if pick <= df.iat[i, 3]:
                seleccionados.append(pop_ranked[i][0])
                break
    return seleccionados

def grupo_apareamiento(poblacion, seleccionados):
    # Crea el grupo de padres según los seleccionados
    return [poblacion[i] for i in seleccionados]

# ---------------------- Reproducción ----------------------
def reproduccion(p1, p2):
    # Cruza dos rutas (padres) para crear un nuevo hijo
    start, end = sorted(random.sample(range(len(p1)), 2))
    hijo = p1[start:end]
    hijo += [c for c in p2 if c not in hijo]
    return hijo

def reproduccion_poblacion(grupo, num_elite):
    # Genera nueva población (mantiene la élite)
    hijos = grupo[:num_elite]
    restantes = len(grupo) - num_elite
    for i in range(restantes):
        hijo = reproduccion(grupo[i], grupo[len(grupo) - i - 1])
        hijos.append(hijo)
    return hijos

# ---------------------- Mutación ----------------------
def mutacion(individuo, tasa):
    # Intercambia dos municipios con cierta probabilidad
    for i in range(len(individuo)):
        if random.random() < tasa:
            j = random.randint(0, len(individuo) - 1)
            individuo[i], individuo[j] = individuo[j], individuo[i]
    return individuo

def mutacion_poblacion(poblacion, tasa):
    # Aplica la mutación a toda la población
    return [mutacion(ind.copy(), tasa) for ind in poblacion]

# ---------------------- Evolución ----------------------
def nueva_generacion(poblacion, num_elite, tasa_mutacion):
    # Genera una nueva generación completa
    pop_ranked = clasificar_rutas(poblacion)
    seleccion = seleccion_rutas(pop_ranked, num_elite)
    grupo = grupo_apareamiento(poblacion, seleccion)
    hijos = reproduccion_poblacion(grupo, num_elite)
    return mutacion_poblacion(hijos, tasa_mutacion)
