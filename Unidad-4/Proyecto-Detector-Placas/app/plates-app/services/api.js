// services/api.js - Llamadas a la API

import { API_URL } from '../config';

export const apiService = {
  /**
   * Consultar propietario por imagen
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
   * Registrar nueva placa
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

  /**
   * Consultar por placa directa (para testing)
   */
  async queryByPlate(plateNumber) {
    try {
      const response = await fetch(`${API_URL}/query-by-plate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          plateNumber: plateNumber.toUpperCase().trim(),
        }),
      });

      const data = await response.json();
      return { success: true, data };
    } catch (error) {
      console.error('Error en queryByPlate:', error);
      return { 
        success: false, 
        error: 'Error de conexión con el servidor' 
      };
    }
  },
};