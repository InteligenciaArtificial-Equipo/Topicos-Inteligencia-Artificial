package pso.main

/** Representa un punto de muestreo o un punto de la malla de referencia dentro
  * del campo agrícola, con sus propiedades físicas y su valor real de humedad.
  *
  * Estos puntos son la base para calcular el error de estimación del sensor.
  *
  * @param humidity
  *   El contenido de humedad del suelo en ese punto, en 0..1.
  * @param crop
  *   El tipo de cultivo o la zona de ponderación asociada a este punto.
  * @param elevation
  *   La elevación o topografía local.
  * @param salinity
  *   El nivel de salinidad del suelo.
  * @param temperature
  *   La temperatura del suelo.
  * @param lat
  *   La coordenada de latitud geográfica.
  * @param lon
  *   La coordenada de longitud geográfica.
  * @param x
  *   Coordenada local X (en metros) dentro de la región de estudio (ej.
  *   500x500m)
  * @param y
  *   Coordenada local Y (en metros) dentro de la región de estudio.
  */
case class ObservedPoint(
    humidity: Double,
    crop: String,
    elevation: Double,
    salinity: Double,
    temperature: Double,
    lat: Double,
    lon: Double,
    x: Option[Double] = None,
    y: Option[Double] = None
)
