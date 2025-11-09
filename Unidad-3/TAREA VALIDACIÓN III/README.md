# TAREA DE VALIDACIÓN III 
# Algoritmo Genético - Optimización de Rutas

# Alumnos:
# Luis Antonio Peñuelas Lopez   
# Eliezer Daniel Peraza Medina

#  Descripción general del proyecto
Este proyecto implementa un algoritmo genético (AG) en Python para resolver un problema de optimización de rutas, basado en el problema del viajero (TSP).  
El objetivo del algoritmo es encontrar la ruta más corta que conecte una serie de municipios, visitando cada uno exactamente una vez y regresando al punto de inicio.

El algoritmo genético utiliza principios inspirados en la evolución biológica:
- Inicialización de la población: Se generan rutas aleatorias como posibles soluciones.
- Evaluación de aptitud (fitness): Se mide qué tan buena es cada ruta, basándose en la distancia total.
- Selección: Se eligen las rutas más aptas para reproducirse.
- Crossover (cruce): Se combinan partes de dos rutas para crear nuevas soluciones.
- Mutación: Se introducen pequeñas variaciones para mantener la diversidad.
- Evolución: Este proceso se repite durante varias generaciones hasta encontrar la mejor ruta posible.


# Instrucciones para ejecutar el código

1. Clonar o copiar el archivo `TAREA VALIDACIÓN III` en tu entorno de trabajo.  
2. Asegúrate de tener Python 3 instalado en tu computadora.  
3. Abre una terminal o consola en la carpeta donde está el archivo.  
4. Instala las dependencias necesarias ejecutando el siguiente comando:

    python install numpy pandas
