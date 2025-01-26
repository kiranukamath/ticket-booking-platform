// Login Form Submission
document.getElementById('login-form')?.addEventListener('submit', function(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.token) {
            localStorage.setItem('token', data.token);
            window.location.href = 'booking.html';
        } else {
            document.getElementById('login-response').textContent = 'Login failed!';
        }
    })
    .catch(error => {
        document.getElementById('login-response').textContent = 'Error: ' + error.message;
    });
});

// Booking Form Submission
document.getElementById('booking-form')?.addEventListener('submit', function(event) {
    event.preventDefault();
    const eventId = document.getElementById('event-id').value;
    const quantity = document.getElementById('quantity').value;

    fetch('/api/bookings', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        body: JSON.stringify({ eventId, quantity })
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('booking-response').textContent = 'Booking successful!';
    })
    .catch(error => {
        document.getElementById('booking-response').textContent = 'Error: ' + error.message;
    });
});

// Fetch Tickets
window.onload = function() {
    if (document.getElementById('tickets-container')) {
        fetch('/api/tickets', {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('tickets-container');
            data.forEach(ticket => {
                const ticketDiv = document.createElement('div');
                ticketDiv.textContent = `Ticket ID: ${ticket.id}, Event ID: ${ticket.eventId}, Quantity: ${ticket.quantity}`;
                container.appendChild(ticketDiv);
            });
        })
        .catch(error => {
            document.getElementById('tickets-container').textContent = 'Error: ' + error.message;
        });
    }
};
