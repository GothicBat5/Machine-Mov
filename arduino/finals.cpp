/* 
1st Led = Pin2
2nd Led = Pin3
3rd Led = Pin4
4th Led = Pin5
5th Led = Pin6
*/

void setup() 
{
  pinMode(PIN2, OUTPUT);
  pinMode(PIN3, OUTPUT);
  pinMode(PIN4, OUTPUT);
  pinMode(PIN5, OUTPUT);
  pinMode(PIN6, OUTPUT);
}

void loop() //delay(milliseconds)
{                   
  digitalWrite(PIN2, HIGH);
  delay(500);

  digitalWrite(PIN3, HIGH);
  delay(500);  

  digitalWrite(PIN4, HIGH);
  delay(500);

  digitalWrite(PIN5, HIGH);
  delay(500);  

  digitalWrite(PIN6, HIGH);
  delay(500);  

  // Cut: High -> Low

  digitalWrite(PIN6, LOW);
  delay(500);

  digitalWrite(PIN5, LOW);
  delay(500); 

  digitalWrite(PIN4, LOW);
  delay(500); 

  digitalWrite(PIN3, LOW);
  delay(500); 

  digitalWrite(PIN2, LOW);
  delay(500); // Loop later <-
}
