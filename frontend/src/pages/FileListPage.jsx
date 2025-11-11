// 💾 src/pages/FileListPage.jsx (AKTUALIZACJA)

import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext'; 

const API_LIST_URL = '/api/files'; 
const API_DELETE_URL = '/api/files'; // Endpoint usuwania to /api/files/{id}

function FileListPage() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  // ⭐️ POBIERZ isADMIN I getToken ⭐️
  const { getToken, isAdmin } = useAuth(); 

  const fetchFiles = async () => {
    setLoading(true);
    const token = getToken();

    if (!token) {
        console.warn("Brak tokena, nie można pobrać listy plików.");
        setLoading(false);
        return; 
    }

    try {
        const response = await fetch(API_LIST_URL, {
            headers: {
                'Authorization': `Bearer ${token}`, 
            }
        });

        if (response.status === 401 || response.status === 403) {
            throw new Error(`Błąd autoryzacji (${response.status}). Upewnij się, że token jest ważny.`);
        }
        if (!response.ok) {
            throw new Error(`Błąd serwera: ${response.statusText}`);
        }

        const data = await response.json();
        setFiles(data); 
    } catch (error) {
        console.error("Błąd pobierania listy plików:", error.message);
    } finally {
        setLoading(false);
    }
  };
  
  // ⭐️ NOWA FUNKCJA DO USUWANIA PLIKÓW ⭐️
  const deleteFile = async (fileId, fileName) => {
    if (!window.confirm(`Czy na pewno chcesz usunąć plik: ${fileName}? Spowoduje to usunięcie zarówno rekordu w bazie, jak i fizycznego pliku z dysku.`)) {
        return;
    }
    
    const token = getToken();

    try {
        const response = await fetch(`${API_DELETE_URL}/${fileId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`, 
            }
        });

        if (response.status === 403) {
            alert('Brak uprawnień. Tylko administrator może usuwać pliki.');
            return;
        }

        if (response.status === 404) {
             alert('Plik nie znaleziony (możliwe, że został już usunięty).');
        }

        if (!response.ok && response.status !== 404 && response.status !== 204) { 
             throw new Error(`Błąd serwera podczas usuwania: ${response.statusText}`);
        }
        
        // Pomyślne usunięcie (204 No Content lub 404)
        alert(`Plik ${fileName} usunięty pomyślnie!`);
        fetchFiles(); // Odśwież listę po usunięciu

    } catch (error) {
        console.error("Błąd usuwania pliku:", error.message);
        alert(`Błąd usuwania: ${error.message}`);
    }
  };
  // ------------------------------------

  useEffect(() => {
    fetchFiles();
  }, []); 

  // Funkcja pomocnicza do formatowania rozmiaru (bez zmian)
  const formatBytes = (bytes, decimals = 2) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }

  if (loading) {
    return <div>Ładowanie listy plików...</div>;
  }

  return (
    <div style={{ padding: '20px' }}>
      <h2>Pliki na serwerze</h2>
      {files.length === 0 ? (
        <p>Brak plików na serwerze.</p>
      ) : (
        <table style={{ width: '100%', color: 'white' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left' }}>Nazwa pliku</th>
              <th style={{ textAlign: 'left' }}>Typ</th>
              <th style={{ textAlign: 'left' }}>Rozmiar</th>
              <th style={{ textAlign: 'left' }}>Pobierz</th>
              {/* ⭐️ NOWA KOLUMNA TYLKO DLA ADMINA ⭐️ */}
              {isAdmin && <th style={{ textAlign: 'left' }}>Usuń</th>} 
            </tr>
          </thead>
          <tbody>
            {files.map(file => (
              <tr key={file.id}>
                <td>{file.originalFileName}</td>
                <td>{file.contentType}</td>
                <td>{formatBytes(file.size)}</td>
                <td>
                  <a 
                    href={`/api/files/download/${file.id}`} 
                    download={file.originalFileName} 
                  >
                    Pobierz
                  </a>
                </td>
                {/* ⭐️ PRZYCISK USUWANIA TYLKO DLA ADMINA ⭐️ */}
                {isAdmin && (
                  <td>
                    <button 
                        onClick={() => deleteFile(file.id, file.originalFileName)}
                        style={{ background: 'red', color: 'white', border: 'none', padding: '5px 10px', cursor: 'pointer', borderRadius: '4px' }}
                    >
                      Usuń
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default FileListPage;