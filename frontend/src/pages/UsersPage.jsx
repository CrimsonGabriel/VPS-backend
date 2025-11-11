// 💾 src/pages/UsersPage.jsx (Z DODANĄ SEKCJĄ ADMINA)

import { useState, useEffect } from 'react';
import {
  Box, Heading, Text, Spinner, Alert, AlertIcon,
  VStack, List, ListItem, Tag, Button, HStack, useToast,
  // Nowe komponenty dla CRUD:
  Modal, ModalOverlay, ModalContent, ModalHeader, ModalBody, ModalFooter,
  ModalCloseButton, FormControl, FormLabel, Input, Checkbox
} from '@chakra-ui/react';
import { useAuth } from '../context/AuthContext';
import { FaEdit, FaTrash, FaUserPlus } from 'react-icons/fa'; // Import ikon
import { WarningIcon } from '@chakra-ui/icons'; // ⭐️ DODANY IMPORT DLA NOWEGO PRZYCISKU

// Endpoint panelu ADMINA
const API_URL = '/api/admin/users';
// ⭐️ DODANY ENDPOINT USUWANIA HISTORII ⭐️
const DELETE_HISTORY_URL = '/api/data/history/delete';

// --- Komponent Formularza (Dodawanie/Edytowanie) ---
// (Ten komponent pozostaje BEZ ZMIAN)
const UserForm = ({ initialData, isOpen, onClose, fetchUsers }) => {
    const toast = useToast();
    const { getToken } = useAuth();
    
    const [formData, setFormData] = useState({
        email: initialData?.email || '',
        name: initialData?.name || '',
        password: '', 
        isAdmin: initialData?.role === 'ADMIN' || false,
    });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (initialData) {
            setFormData({
                email: initialData.email || '',
                name: initialData.name || '',
                password: '',
                isAdmin: initialData.role === 'ADMIN' || false,
            });
        } else {
            setFormData({ email: '', name: '', password: '', isAdmin: false });
        }
    }, [initialData]);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        const token = getToken();

        if (!initialData && !formData.password) {
            toast({
                title: 'Błąd walidacji',
                description: 'Hasło jest wymagane przy tworzeniu nowego użytkownika.',
                status: 'warning',
                duration: 5000,
                isClosable: true,
            });
            setLoading(false);
            return;
        }

        if (!formData.email) {
            toast({
                title: 'Błąd walidacji',
                description: 'Email jest wymagany.',
                status: 'warning',
                duration: 5000,
                isClosable: true,
            });
            setLoading(false);
            return;
        }


        const method = initialData ? 'PUT' : 'POST';
        const url = initialData ? `${API_URL}/${initialData.id}` : API_URL;

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Błąd HTTP: ${response.status}. Odpowiedź: ${errorText}`);
            }

            toast({
                title: initialData ? 'Edycja pomyślna' : 'Utworzenie pomyślne',
                description: `Użytkownik ${formData.email} został zapisany.`,
                status: 'success',
                duration: 3000,
                isClosable: true,
            });

            fetchUsers();
            onClose(); 

        } catch (err) {
            toast({
                title: 'Błąd operacji',
                description: err.message,
                status: 'error',
                duration: 5000,
                isClosable: true,
            });
        } finally {
            setLoading(false);
        }
    };


    return (
        <Modal isOpen={isOpen} onClose={onClose}>
            <ModalOverlay />
            <ModalContent>
                <ModalHeader>{initialData ? 'Edytuj Użytkownika' : 'Dodaj Użytkownika'}</ModalHeader>
                <ModalCloseButton />
                <form onSubmit={handleSubmit}>
                    <ModalBody pb={6}>
                        <FormControl isRequired>
                            <FormLabel>Email</FormLabel>
                            <Input 
                                placeholder="E-mail" 
                                name="email" 
                                value={formData.email} 
                                onChange={handleChange} 
                                isDisabled={!!initialData} 
                            />
                        </FormControl>

                        <FormControl mt={4}>
                            <FormLabel>Imię/Nazwa</FormLabel>
                            <Input 
                                placeholder="Imię lub Nickname" 
                                name="name" 
                                value={formData.name} 
                                onChange={handleChange} 
                            />
                        </FormControl>

                        <FormControl mt={4} isRequired={!initialData}>
                            <FormLabel>Hasło</FormLabel>
                            <Input 
                                placeholder={initialData ? 'Zostaw puste, aby nie zmieniać' : 'Wpisz hasło'}
                                type="password" 
                                name="password" 
                                value={formData.password} 
                                onChange={handleChange} 
                            />
                        </FormControl>

                        <FormControl mt={4}>
                            <Checkbox 
                                name="isAdmin" 
                                isChecked={formData.isAdmin} 
                                onChange={handleChange}
                                colorScheme="red"
                                isDisabled={initialData?.email === 'admin'} 
                            >
                                Użytkownik jest administratorem (ADMIN)
                            </Checkbox>
                        </FormControl>
                        
                    </ModalBody>

                    <ModalFooter>
                        <Button colorScheme="blue" mr={3} type="submit" isLoading={loading}>
                            {initialData ? 'Zapisz zmiany' : 'Dodaj'}
                        </Button>
                        <Button onClick={onClose}>Anuluj</Button>
                    </ModalFooter>
                </form>
            </ModalContent>
        </Modal>
    );
};
// --- Koniec Komponentu Formularza ---


function UsersPage() {
  const [users, setUsers] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  
  // Stany dla Modala
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  
  // ⭐️ DODANY STAN DLA USUWANIA HISTORII ⭐️
  const [isDeleting, setIsDeleting] = useState(false);

  // ⭐️ POPRAWKA: Pobieramy 'isAdmin' (dla przycisku) i dodajemy 'toast'
  const { getToken, isAuthenticated, loading: authLoading, logout, isAdmin } = useAuth();
  const toast = useToast();

  const fetchUsers = async () => {
    setLoading(true); 
    try {
      const token = getToken();
      
      if (!token) {
        logout(); 
        return;
      }
      
      const response = await fetch(API_URL, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      
      if (response.status === 401) {
          throw new Error("Błąd 401: Sesja wygasła. Proszę zalogować się ponownie.");
      }
      
      if (response.status === 403) {
        throw new Error("Brak uprawnień. Tylko użytkownik z rolą ADMIN ma dostęp. (Error 403)");
      }
      
      if (!response.ok) {
        const errorText = await response.text();
        let errorMsg = `Błąd HTTP: ${response.status}.`;
        try {
            const errorJson = JSON.parse(errorText);
            errorMsg += ` Odpowiedź: ${errorJson.message || errorJson.error || errorText}`;
        } catch (e) {
            errorMsg += ` Odpowiedź: ${errorText}`;
        }
        throw new Error(errorMsg);
      }
      
      const data = await response.json()
      setUsers(data)
      setError(null)

    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  // Użycie: Otwiera modal w trybie edycji
  const handleOpenEdit = (user) => {
    setEditingUser(user);
    setIsModalOpen(true);
  };

  // Użycie: Otwiera modal w trybie dodawania
  const handleOpenCreate = () => {
    setEditingUser(null);
    setIsModalOpen(true);
  };

  // Użycie: Zamyka modal
  const handleClose = () => {
    setIsModalOpen(false);
    setEditingUser(null);
  };

  // Użycie: Usuwanie użytkownika (logika bez zmian)
  const handleDelete = async (id) => {
    const token = getToken();

    if (!window.confirm("Czy na pewno chcesz usunąć tego użytkownika?")) {
        return;
    }

    try {
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.status === 204) { 
            toast({
                title: 'Usunięto pomyślnie.',
                description: `Użytkownik o ID ${id} został usunięty.`,
                status: 'success',
                duration: 3000,
                isClosable: true,
            });
            fetchUsers();
        } else if (response.status === 403) {
            throw new Error("Brak uprawnień do usunięcia (lub próba usunięcia głównego admina).");
        } else {
            const errorText = await response.text();
            throw new Error(`Błąd HTTP: ${response.status}. Odpowiedź: ${errorText}`);
        }

    } catch (err) {
        toast({
            title: 'Błąd usuwania',
            description: err.message,
            status: 'error',
            duration: 5000,
            isClosable: true,
        });
    }
  };
  
  // ⭐️ DODANA FUNKCJA USUWANIA HISTORII (przeniesiona ze StatusIpPage) ⭐️
  const handleDeleteHistory = async () => {
    if (!window.confirm("Czy na pewno chcesz usunąć CAŁĄ historię czujników z serwera? Tej akcji nie można cofnąć.")) {
        return;
    }

    setIsDeleting(true);
    const token = getToken(); // Używamy funkcji z Twojego AuthContext

    try {
        const response = await fetch(DELETE_HISTORY_URL, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Błąd serwera: ${response.status}`);
        }

        toast({
            title: "Sukces!",
            description: "Historia czujników została pomyślnie usunięta.",
            status: "success",
            duration: 5000,
            isClosable: true,
        });

    } catch (error) {
        toast({
            title: "Błąd podczas usuwania",
            description: `Wystąpił błąd: ${error.message}`,
            status: "error",
            duration: 5000,
            isClosable: true,
        });
    } finally {
        setIsDeleting(false);
    }
  };


  useEffect(() => {
    if (!authLoading && isAuthenticated) {
        fetchUsers()
    } else if (!authLoading && !isAuthenticated) {
        setLoading(false);
    }
  }, [isAuthenticated, authLoading]); 

  if (authLoading) {
      return <div>Weryfikacja kontekstu...</div>;
  }
  
  if (loading && !users) {
    return <Spinner size="xl" />
  }

  if (error) {
    return (
      <Alert status="error">
        <AlertIcon />
        Błąd: {error}
      </Alert>
    )
  }

  const userList = users || [] 

  return (
    <Box p={5} shadow="md" borderWidth="1px" borderRadius="md" bg="gray.800">
      <Heading size="lg" mb={4}>Zarządzanie Użytkownikami (ADMIN CRUD)</Heading>
      
      <Button 
        colorScheme="green" 
        leftIcon={<FaUserPlus />} 
        mb={4}
        onClick={handleOpenCreate}
      >
        Dodaj Nowego Użytkownika
      </Button>

      {userList.length > 0 ? (
        <List spacing={3}>
          {userList.map(user => (
            <ListItem 
              key={user.id} 
              p={3} 
              bg="gray.700" 
              borderRadius="md" 
              display="flex" 
              flexDirection={{ base: 'column', sm: 'row' }}
              justifyContent="space-between" 
              alignItems={{ base: 'flex-start', sm: 'center' }}
            >
              <VStack align="flex-start" spacing={1}>
                <Text fontWeight="bold">{user.email}</Text>
                <Text fontSize="sm" color="gray.400">{user.name}</Text>
              </VStack>

              <HStack spacing={3} mt={{ base: 2, sm: 0 }}>
                <Tag 
                    size="sm" 
                    colorScheme={user.role === 'ADMIN' ? 'red' : 'blue'}
                >
                    {user.role}
                </Tag>
                {user.twoFactorEnabled ? (
                    <Tag size="sm" colorScheme="green">2FA Włączone</Tag>
                ) : (
                    <Tag size="sm" colorScheme="yellow">Brak 2FA</Tag>
                )}
                <Button size="sm" leftIcon={<FaEdit />} onClick={() => handleOpenEdit(user)}>
                    Edytuj
                </Button>
                <Button 
                    size="sm" 
                    colorScheme="red" 
                    leftIcon={<FaTrash />} 
                    onClick={() => handleDelete(user.id)} 
                    isDisabled={user.email === 'admin'} 
                >
                    Usuń
                </Button>
              </HStack>
            </ListItem>
          ))}
        </List>
      ) : (
        <Text color="gray.400">Brak zarejestrowanych użytkowników do zarządzania.</Text>
      )}
      
      {/* ⭐️ DODANA SEKCJA ADMINA (przeniesiona ze StatusIpPage) ⭐️ */}
      {isAdmin && (
        <Box mt={8} p={5} shadow="md" borderWidth="1px" borderRadius="md" bg="gray.900" borderColor="red.500">
            <Heading size="md" mb={3} color="red.300">Inne Akcje Administracyjne</Heading>
            <Text mb={4}>Ta akcja jest nieodwracalna i usunie wszystkie dane czujników z bazy danych VPS.</Text>
            <Button
                colorScheme="red"
                isLoading={isDeleting}
                onClick={handleDeleteHistory}
                leftIcon={<WarningIcon />}
            >
                Usuń całą historię czujników z VPS
            </Button>
        </Box>
      )}

      {/* Komponent Modala (poza głównym Boxem) */}
      <UserForm 
          initialData={editingUser} 
          isOpen={isModalOpen} 
          onClose={handleClose} 
          fetchUsers={fetchUsers} 
      />
    </Box>
  )
}

export default UsersPage;