
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// ⭐️ Dodajemy prop requiredRole ⭐️
function ProtectedRoute({ requiredRole }) {
    const { isAuthenticated, userRole, loading } = useAuth();
    
    if (loading) {
        return <div>Weryfikacja dostępu...</div>;
    }

    // 1. Sprawdzenie uwierzytelnienia
    if (!isAuthenticated) {
        // Przekierowanie do logowania, jeśli brak tokena
        return <Navigate to="/login" replace />; 
    }

    // 2. Sprawdzenie autoryzacji (jeśli wymagana jest konkretna rola)
    if (requiredRole && userRole !== requiredRole) {
        // Przekierowanie do 403 Forbidden
        return <Navigate to="/forbidden" replace />;
    }

    // Dostęp dozwolony
    return <Outlet />;
}

export default ProtectedRoute;