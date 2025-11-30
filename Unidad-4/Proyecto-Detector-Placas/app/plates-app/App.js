// App.js - Aplicación Principal

import React, { useState } from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import QueryScreen from './screens/QueryScreen';
import RegisterScreen from './screens/RegisterScreen';
import { styles } from './styles';

export default function App() {
  const [screen, setScreen] = useState('query'); // 'query' o 'register'

  return (
    <View style={styles.container}>
      {/* Barra de navegación superior */}
      <View style={styles.navbar}>
        <TouchableOpacity
          style={[styles.navButton, screen === 'query' && styles.navButtonActive]}
          onPress={() => setScreen('query')}
        >
          <Text style={[styles.navText, screen === 'query' && styles.navTextActive]}>
            Consultar
          </Text>
        </TouchableOpacity>
        
        <TouchableOpacity
          style={[styles.navButton, screen === 'register' && styles.navButtonActive]}
          onPress={() => setScreen('register')}
        >
          <Text style={[styles.navText, screen === 'register' && styles.navTextActive]}>
            Registrar
          </Text>
        </TouchableOpacity>
      </View>

      {/* Contenido */}
      {screen === 'query' ? <QueryScreen /> : <RegisterScreen />}
    </View>
  );
}