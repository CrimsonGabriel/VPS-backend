import React, { useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
// Prosty styl dla strefy upuszczania
const dropzoneStyle = {
  flex: 1,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  padding: '40px',
  borderWidth: 2,
  borderRadius: 5,
  borderColor: '#666',
  borderStyle: 'dashed',
  backgroundColor: '#333',
  color: '#eee',
  outline: 'none',
  transition: 'border .24s ease-in-out'
};

function FileUploadPage() {
  const { getToken, logout } = useAuth(); // ⭐️ UŻYCIE ⭐️

  const onDrop = useCallback(acceptedFiles => {
    
    const token = getToken();
    if (!token) {
        console.error('Błąd wysyłania pliku: Brak tokena JWT.');
        alert('Błąd: Brak uwierzytelnienia. Zaloguj się ponownie.');
        logout(); // Wymuś wylogowanie/przekierowanie
        return;
    }

    acceptedFiles.forEach(file => {
      
      const formData = new FormData();
      formData.append('file', file); 

      // Wysyłamy plik do Spring Boota
      axios.post('/api/files/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          // ⭐️ KLUCZOWA POPRAWKA: AUTORYZACJA ⭐️
          'Authorization': `Bearer ${token}` 
        }
      })
      .then(response => {
        console.log(response.data);
        alert('Plik wgrany pomyślnie!');
      })
      .catch(error => {
        console.error('Błąd wysyłania pliku:', error.response?.data?.message || error.message);
        alert('Błąd wysyłania pliku: ' + (error.response?.data?.message || 'Sprawdź konsolę.'));
      });
    });
  }, [getToken, logout]); // Dodaj dependency array

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop });

  return (
    <div style={{ padding: '20px' }}>
      <h2>Wyślij plik na serwer</h2>
      <div {...getRootProps({ style: dropzoneStyle })}>
        <input {...getInputProps()} />
        {
          isDragActive ?
            <p>Upuść pliki tutaj ...</p> :
            <p>Przeciągnij i upuść pliki tutaj, albo kliknij aby wybrać pliki</p>
        }
      </div>
    </div>
  );
}

export default FileUploadPage;
