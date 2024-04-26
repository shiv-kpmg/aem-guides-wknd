$(document).ready(function () {
    $(".wknd-registration-form").on("submit", function (event) {
        event.preventDefault();

        let formData = $(this).serialize();

        // Make an ajax call to AEM servlet
        $.ajax({
            type: 'POST',
            // Replace with the actual URL of your servlet
            url: '/content/wknd/us/en/sign-up/jcr:content/root/container/user_registration.register.json',
            data: {
                userData: formData
            },
            success: function (response) {
                //clearing the form data
                $('.wknd-registration-form')[0].reset();

                if (response.creationFlag === true) {
                    $("#newuser-popup").modal('show');
                } else {
                    $("#existing-popup").modal('show');
                }
            },
            error: function (xhr, status, error) {
                //clearing the form data
                $('.wknd-registration-form')[0].reset();
                window.location.href = '/content/wknd/us/en/sign-up.html';
            }
        });
    });

    //Continue login
    $(document).on("click", ".login-redirection", function() {
        //redirecting to sign up page
        $(".success-popup").addClass("d-none");
        window.location.href = '/content/wknd/us/en/errors/sign-in.html';
      });
});
