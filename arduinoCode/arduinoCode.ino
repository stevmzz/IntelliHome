#include <Servo.h>
#include <DHT.h>

// Configuración del sensor DHT11
#define DHTPIN 2     // Pin digital para el sensor DHT11
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// Configuración de LEDs y puertas
const int NUM_LEDS = 8;
const int NUM_DOORS = 6;
const int ledPins[NUM_LEDS] = {4, 5, 6, 7, 8, 9, 10, 11};
const int servoPins[NUM_DOORS] = {A2, A5, A3, A4, A0, A1};  // Pines para los servomotores

Servo doorServos[NUM_DOORS];  // Array de objetos Servo

// Posiciones para los servos
const int CLOSED_POSITION = 0;    // Posición cerrada
const int OPEN_POSITION = 120;    // Posición abierta

// Variables para el sensor DHT11
unsigned long lastDHTRead = 0;
const long DHT_INTERVAL = 2000; // Intervalo de lectura del DHT11 (2 segundos)

void setup() {
  Serial.begin(9600);
  
  // Inicializar sensor DHT11
  dht.begin();
  
  // Configurar LEDs
  for(int i = 0; i < NUM_LEDS; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW);
  }

  // Configurar Servomotores
  for(int i = 0; i < NUM_DOORS; i++) {
    doorServos[i].attach(servoPins[i]);
    doorServos[i].write(CLOSED_POSITION);  // Iniciar todas las puertas cerradas
  }
}

void loop() {
  // Manejar comandos seriales
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    int separatorIndex = command.indexOf(':');
    if(separatorIndex != -1) {
      String cmd = command.substring(0, separatorIndex);
      String param = command.substring(separatorIndex + 1);
      
      // Manejar comandos LED
      if(cmd == "LED_ON" || cmd == "LED_OFF") {
        handleLEDCommand(cmd, param.toInt());
      }
      // Manejar comandos de puerta
      else if(cmd == "DOOR_OPEN" || cmd == "DOOR_CLOSE") {
        handleDoorCommand(cmd, param);
      }
      // Manejar comando de lectura DHT11
      else if(cmd == "READ_DHT") {
        readAndSendDHTData();
      }
    }
    Serial.flush();
  }

  // Lectura periódica del sensor DHT11 (cada 2 segundos)
  if (millis() - lastDHTRead >= DHT_INTERVAL) {
    readAndSendDHTData();
    lastDHTRead = millis();
  }
}

void handleLEDCommand(String cmd, int ledIndex) {
  if(ledIndex >= 0 && ledIndex < NUM_LEDS) {
    if(cmd == "LED_ON") {
      digitalWrite(ledPins[ledIndex], HIGH);
      Serial.println("SUCCESS:LED_ON");
    }
    else if(cmd == "LED_OFF") {
      digitalWrite(ledPins[ledIndex], LOW);
      Serial.println("SUCCESS:LED_OFF");
    }
  }
}

void handleDoorCommand(String cmd, String doorName) {
  int doorIndex = getDoorIndex(doorName);
  
  if(doorIndex >= 0 && doorIndex < NUM_DOORS) {
    if(cmd == "DOOR_OPEN") {
      doorServos[doorIndex].write(OPEN_POSITION);
      Serial.println("SUCCESS:DOOR_OPEN:" + doorName);
    }
    else if(cmd == "DOOR_CLOSE") {
      doorServos[doorIndex].write(CLOSED_POSITION);
      Serial.println("SUCCESS:DOOR_CLOSE:" + doorName);
    }
  }
}

int getDoorIndex(String doorName) {
  if(doorName == "GARAGE") return 0;
  if(doorName == "MAIN") return 1;
  if(doorName == "KITCHEN") return 2;
  if(doorName == "LIVING") return 3;
  if(doorName == "BEDROOM1") return 4;
  if(doorName == "BEDROOM2") return 5;
  return -1;
}

void readAndSendDHTData() {
  float humedad = dht.readHumidity();
  float temperatura = dht.readTemperature();

  if (isnan(humedad) || isnan(temperatura)) {
    Serial.println("ERROR:DHT_READ_FAILED");
    return;
  }

  // Enviar datos en formato JSON para facilitar el parsing
  Serial.print("DHT_DATA:");
  Serial.print("{\"temperatura\":");
  Serial.print(temperatura);
  Serial.print(",\"humedad\":");
  Serial.print(humedad);
  Serial.println("}");
}