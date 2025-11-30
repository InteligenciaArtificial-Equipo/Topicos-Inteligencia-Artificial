// services/api.js - Llamadas a la API

import { API_URL } from '../config';


/**
 * apiService
 *
 * Objeto que centraliza todas las llamadas HTTP al backend.
 * Cada método devuelve un objeto con la forma:
 * {
 *   success: boolean,
 *   data?: any,
 *   error?: string
 * }
 */
export const apiService = {
  
  /**
   * queryByImage
   *
   * Envía una imagen codificada en Base64 al backend para
   * realizar el reconocimiento de la placa y consultar
   * el propietario correspondiente.
   *
   * Endpoint: POST /query
   *
   * @param {string} imageBase64 - Imagen codificada en Base64.
   * @returns {Promise<{success: boolean, data?: any, error?: string}>}
   *          - success: true si la petición se completó correctamente.
   *          - data: respuesta del backend con el resultado.
   *          - error: mensaje de error si falló la conexión.
   */
  async queryByImage(imageBase64) {
    try {
      const response = await fetch(`${API_URL}/query`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          imageBase64: imageBase64,
        }),
      });

      const data = await response.json();
      return { success: true, data };
    } catch (error) {
      console.error('Error en queryByImage:', error);
      return { 
        success: false, 
        error: 'Error de conexión con el servidor' 
      };
    }
  },

  /**
   * registerPlate
   *
   * Envía al backend los datos de una nueva placa y su propietario
   * para registrarlos en la base de datos.
   *
   * Endpoint: POST /register
   *
   * @param {string} plateNumber - Número de placa a registrar.
   * @param {string} ownerName - Nombre del propietario.
   * @returns {Promise<{success: boolean, data?: any, error?: string}>}
   *          - success: true si la petición se completó correctamente.
   *          - data: respuesta del backend.
   *          - error: mensaje de error si la conexión falló.
   */
  async registerPlate(plateNumber, ownerName) {
    try {
      const response = await fetch(`${API_URL}/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          plateNumber: plateNumber.toUpperCase().trim(),
          ownerName: ownerName.trim(),
        }),
      });

      const data = await response.json();
      return { success: true, data };
    } catch (error) {
      console.error('Error en registerPlate:', error);
      return { 
        success: false, 
        error: 'Error de conexión con el servidor' 
      };
    }
  },
};
