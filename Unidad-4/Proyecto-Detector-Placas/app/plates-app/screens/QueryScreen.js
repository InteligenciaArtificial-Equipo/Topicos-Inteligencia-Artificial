// screens/QueryScreen.js

import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Image,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { apiService } from '../services/api';
import { CONFIG } from '../config';
import { styles } from '../styles';

export default function QueryScreen() {
  const [image, setImage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const takePhoto = async () => {
    // Pedir permisos
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    
    if (status !== 'granted') {
      Alert.alert('Permiso denegado', 'Necesitamos acceso a la cámara');
      return;
    }

    // Tomar foto
    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      aspect: CONFIG.imageAspect,
      quality: CONFIG.imageQuality,
      base64: true,
    });

    if (!result.canceled && result.assets[0]) {
      setImage(result.assets[0]);
      queryPlate(result.assets[0].base64);
    }
  };

  const queryPlate = async (imageBase64) => {
    setLoading(true);
    setResult(null);

    const response = await apiService.queryByImage(imageBase64);

    if (response.success && response.data.success && response.data.data) {
      setResult(response.data.data);
    } else {
      Alert.alert(
        'No encontrado', 
        response.data?.message || 'No se pudo reconocer la placa'
      );
    }

    setLoading(false);
  };

  return (
    <ScrollView style={styles.content}>
      <Text style={styles.title}>Consultar Propietario</Text>
      <Text style={styles.subtitle}>Toma una foto de la placa del vehículo</Text>

      <TouchableOpacity style={styles.photoButton} onPress={takePhoto}>
        <Text style={styles.photoButtonText}>📷 Tomar Foto</Text>
      </TouchableOpacity>

      {image && (
        <View style={styles.imageContainer}>
          <Image source={{ uri: image.uri }} style={styles.previewImage} />
        </View>
      )}

      {loading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#007AFF" />
          <Text style={styles.loadingText}>Analizando placa...</Text>
        </View>
      )}

      {result && (
        <View style={styles.resultCard}>
          <Text style={styles.resultTitle}>Propietario Encontrado</Text>
          
          <View style={styles.resultRow}>
            <Text style={styles.resultLabel}>Placa:</Text>
            <Text style={styles.resultValue}>{result.plate.plateNumber}</Text>
          </View>
          
          <View style={styles.resultRow}>
            <Text style={styles.resultLabel}>Propietario:</Text>
            <Text style={styles.resultValue}>{result.owner.name}</Text>
          </View>
        </View>
      )}
    </ScrollView>
  );
}