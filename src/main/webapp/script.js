$(function() {
    $(".more").click(function(event) {
      event.preventDefault();
      var thumbsCount = $(".thumb").size();
      $.get("imagescan?more=ajax&fromIndex="+thumbsCount, function(response) {
          var resp = $(response);
          if (resp) {
            $('#thumbnails').append($('.thumb', resp));

            if (!$('.more', resp).size()){
              $('.more').hide();
            }
          } else {
            $('.more').hide();
          }
      });
    });
});

