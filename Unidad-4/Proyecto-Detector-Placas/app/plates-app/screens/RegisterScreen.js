// screens/RegisterScreen.js

import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { apiService } from '../services/api';
import { CONFIG } from '../config';
import { styles } from '../styles';

export default function RegisterScreen() {
  const [plateNumber, setPlateNumber] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [loading, setLoading] = useState(false);

  const registerPlate = async () => {
    // Validaciones
    if (!plateNumber.trim()) {
      Alert.alert('Falta la placa', 'Por favor ingresa el número de placa');
      return;
    }
    
    if (!ownerName.trim()) {
      Alert.alert('Falta el nombre', 'Por favor ingresa el nombre del propietario');
      return;
    }

    setLoading(true);

    const response = await apiService.registerPlate(plateNumber, ownerName);

    if (response.success && response.data.success) {
      Alert.alert('Éxito', 'Placa registrada correctamente', [
        {
          text: 'OK',
          onPress: () => {
            setPlateNumber('');
            setOwnerName('');
          },
        },
      ]);
    } else {
      Alert.alert(
        'Error', 
        response.data?.message || response.error || 'No se pudo registrar la placa'
      );
    }

    setLoading(false);
  };

  return (
    <ScrollView style={styles.content}>
      <Text style={styles.title}>Registrar Placa</Text>
      <Text style={styles.subtitle}>Ingresa los datos de la placa y propietario</Text>

      <View style={styles.form}>
        <Text style={styles.inputLabel}>Número de Placa *</Text>
        <TextInput
          style={styles.input}
          placeholder="Ejemplo: ABC123"
          value={plateNumber}
          onChangeText={setPlateNumber}
          autoCapitalize="characters"
          maxLength={CONFIG.maxPlateLength}
        />

        <Text style={styles.inputLabel}>Nombre del Propietario *</Text>
        <TextInput
          style={styles.input}
          placeholder="Nombre completo"
          value={ownerName}
          onChangeText={setOwnerName}
        />

        <TouchableOpacity
          style={styles.registerButton}
          onPress={registerPlate}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.registerButtonText}>Registrar Placa</Text>
          )}
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}
